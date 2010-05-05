package de.saumya.mojo.gem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.util.FileUtils;

import de.saumya.mojo.jruby.AbstractJRubyMojo;

/**
 */
public abstract class AbstractGemMojo extends AbstractJRubyMojo {

    /**
     * @parameter expression="${settings.offline}"
     */
    private boolean                    offline;

    /**
     * allow to overwrite the version by explicitly declaring a dependency in
     * the pom. will not check any dependencies on gemspecs level.
     *
     * @parameter expression="${gem.forceVersion}" default-value="false"
     */
    private boolean                    forceVersion;
    
    /** 
     * follow transitive dependencies when initializing rubygem dependencies.
     * 
     * @parameter expression="${gem.useTransitiveDependencies}" default-value="false"
     */
    boolean useTransitiveDependencies;

    /**
     * @parameter default-value="${plugin.artifacts}"
     */
    protected java.util.List<Artifact> pluginArtifacts;

    public class UpdateCheckManager {

        public static final String  LAST_UPDATE_TAG = ".lastUpdated";

        private static final String TOUCHFILE_NAME  = "resolver-status.properties";

        public void touch(final Artifact artifact,
                final ArtifactRepository repository) {
            final File file = artifact.getFile();

            final File touchfile = getTouchfile(artifact);

            if (file.exists()) {
                touchfile.delete();
            }
            else {
                writeLastUpdated(touchfile, getRepositoryKey(repository));
            }
        }

        public void touch(final RepositoryMetadata metadata,
                final ArtifactRepository repository, final File file) {
            final File touchfile = getTouchfile(metadata, file);

            final String key = getMetadataKey(repository, file);

            writeLastUpdated(touchfile, key);
        }

        String getMetadataKey(final ArtifactRepository repository,
                final File file) {
            return repository.getId() + '.' + file.getName() + LAST_UPDATE_TAG;
        }

        String getRepositoryKey(final ArtifactRepository repository) {
            final StringBuilder buffer = new StringBuilder(256);

            // consider the URL (instead of the id) as this most closely relates
            // to the contents in the repo
            buffer.append(repository.getUrl());

            return buffer.toString();
        }

        private void writeLastUpdated(final File touchfile, final String key) {
            synchronized (touchfile.getAbsolutePath().intern()) {
                if (!touchfile.getParentFile().exists()
                        && !touchfile.getParentFile().mkdirs()) {
                    getLog().debug("Failed to create directory: "
                            + touchfile.getParent()
                            + " for tracking artifact metadata resolution.");
                    return;
                }

                FileChannel channel = null;
                FileLock lock = null;
                try {
                    final Properties props = new Properties();

                    channel = new RandomAccessFile(touchfile, "rw").getChannel();
                    lock = channel.lock(0, channel.size(), false);

                    if (touchfile.canRead()) {
                        getLog().debug("Reading resolution-state from: "
                                + touchfile);
                        final ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());

                        channel.read(buffer);
                        buffer.flip();

                        final ByteArrayInputStream stream = new ByteArrayInputStream(buffer.array());
                        props.load(stream);
                    }

                    props.setProperty(key,
                                      Long.toString(System.currentTimeMillis()));

                    final ByteArrayOutputStream stream = new ByteArrayOutputStream();

                    getLog().debug("Writing resolution-state to: " + touchfile);
                    props.store(stream, "Last modified on: " + new Date());

                    final byte[] data = stream.toByteArray();
                    final ByteBuffer buffer = ByteBuffer.allocate(data.length);
                    buffer.put(data);
                    buffer.flip();

                    channel.position(0);
                    channel.write(buffer);
                }
                catch (final IOException e) {
                    getLog().debug("Failed to record lastUpdated information for resolution.\nFile: "
                                           + touchfile.toString()
                                           + "; key: "
                                           + key,
                                   e);
                }
                finally {
                    if (lock != null) {
                        try {
                            lock.release();
                        }
                        catch (final IOException e) {
                            getLog().debug("Error releasing exclusive lock for resolution tracking file: "
                                                   + touchfile,
                                           e);
                        }
                    }

                    if (channel != null) {
                        try {
                            channel.close();
                        }
                        catch (final IOException e) {
                            getLog().debug("Error closing FileChannel for resolution tracking file: "
                                                   + touchfile,
                                           e);
                        }
                    }
                }
            }
        }

        File getTouchfile(final Artifact artifact) {
            final StringBuilder sb = new StringBuilder(128);
            sb.append(artifact.getArtifactId());
            sb.append('-').append(artifact.getBaseVersion());
            if (artifact.getClassifier() != null) {
                sb.append('-').append(artifact.getClassifier());
            }
            sb.append('.').append(artifact.getType()).append(LAST_UPDATE_TAG);
            return new File(artifact.getFile().getParentFile(), sb.toString());
        }

        File getTouchfile(final RepositoryMetadata metadata, final File file) {
            return new File(file.getParent(), TOUCHFILE_NAME);
        }

    }

    private static final int               ONE_DAY_IN_MILLIS  = 86400000;
    private final List<ArtifactRepository> gemRepositories    = new ArrayList<ArtifactRepository>();

    private final UpdateCheckManager       updateCheckManager = new UpdateCheckManager();

    public void execute() throws MojoExecutionException {
        if (this.project.getBasedir() == null) {
            this.gemHome = new File(this.gemHome.getAbsolutePath()
                    .replace("${project.basedir}", ""));
            this.gemPath = new File(this.gemPath.getAbsolutePath()
                    .replace("${project.basedir}", ""));
        }
        execute(this.pluginArtifacts);
        executeWithGems();
    }

    public void execute(final Collection<Artifact> artifacts)
            throws MojoExecutionException {
        for (final ArtifactRepository repository : this.remoteRepositories) {
            // instanceof does not work probably a classloader issue !!!
            if (repository.getLayout()
                    .getClass()
                    .getName()
                    .equals(GemRepositoryLayout.class.getName())) {
                this.gemRepositories.add(repository);
            }
        }
        if (this.gemRepositories.size() == 0) {
            final ArtifactRepositoryPolicy releases = new ArtifactRepositoryPolicy();
            releases.setChecksumPolicy("ignore");
            releases.setUpdatePolicy("never");
            final ArtifactRepositoryPolicy snapshots = new ArtifactRepositoryPolicy();
            snapshots.setEnabled(false);

            final DefaultArtifactRepository rubygemsRepo = new DefaultArtifactRepository("rubygems",
                    "http://rubygems.org/gems",
                    new GemRepositoryLayout(),
                    snapshots,
                    releases);
            getLog().info("gem plugin configured but no gem repository found - fall back to "
                    + rubygemsRepo.getUrl());
            this.gemRepositories.add(rubygemsRepo);
        }
        final File gemsDir = new File(this.gemPath, "gems");

        final StringBuilder gems = new StringBuilder();
        final Map<String, Artifact> collectedArtifacts = new LinkedHashMap<String, Artifact>();
        for (final Artifact artifact : artifacts) {
            if (artifact.getType().contains("gem")
                    || artifact == this.project.getArtifact()) {
                collectArtifacts(artifact, collectedArtifacts, true);
            }
        }
        collectedArtifacts.remove(key(this.project.getArtifact()));
        
        String extraFlag = "";
        if (this.forceVersion) {
            // allow to overwrite resolved version with version of project
            // dependencies
            for (final Object o : this.project.getDependencies()) {
                final Dependency artifact = (Dependency) o;
                final Artifact a = collectedArtifacts.get(artifact.getGroupId()
                        + ":" + artifact.getArtifactId());
                if (!a.getVersion().equals(artifact.getVersion())) {
                    extraFlag = "--force";
                    a.setVersion(artifact.getVersion());
                    a.setResolved(false);
                    a.setFile(null);
                    try {
                        this.resolver.resolve(a,
                                              this.remoteRepositories,
                                              this.localRepository);
                    }
                    catch (final ArtifactResolutionException e) {
                        throw new MojoExecutionException("error resolving " + a,
                                e);
                    }
                    catch (final ArtifactNotFoundException e) {
                        throw new MojoExecutionException("error resolving " + a,
                                e);
                    }
                }
            }
        }

        // collect all uninstalled gems in a reverse dependency order
        for (final Artifact collectedArtifact : collectedArtifacts.values()) {
            if (collectedArtifact.getType().contains("gem")) {
                final String prefix = collectedArtifact.getGroupId()
                        .equals("rubygems")
                        ? ""
                        : collectedArtifact.getGroupId() + ".";
                final File gemDir = new File(gemsDir, prefix
                        + (collectedArtifact.getFile()
                                .getName()
                                .replaceAll(".gem$", "").replace("-SNAPSHOT",
                                                                 "")));
                // TODO force flag to install gems via command line
                // argument
                if (!(this.fork && gemDir.exists())) {
                    gems.append(" ").append(collectedArtifact.getFile()
                            .getAbsolutePath());
                }
                else {
                    getLog().debug("already installed: " + collectedArtifact);
                }
            }
        }
        if (gems.length() > 0) {
            execute("-S gem install --no-ri --no-rdoc " + extraFlag + " -l "
                    + gems, false);
        }
        else {
            getLog().debug("no gems found to install");
        }
    }

    abstract protected void executeWithGems() throws MojoExecutionException;

    private boolean createMissingPom(final Artifact artifact)
            throws MojoExecutionException {
        final File pom = new File(artifact.getFile()
                .getPath()
                .replaceFirst("(-java)?.gem$", ".pom"));
        if (artifact.getGroupId().equals("rubygems")) {

            boolean isPom = false;
            if (pom.lastModified() == artifact.getFile().lastModified()) {
                try {
                    isPom = FileUtils.fileRead(pom).startsWith("<?xml");
                }
                catch (final IOException e) {
                    // well just create a new pom now
                }
            }
            if (!isPom) {
                if (this.offline) {
                    getLog().debug("<gems> offline mode - skip creating pom for "
                            + artifact);
                }
                else {
                    getLog().debug("<gems> creating pom for " + artifact);
                    // use temporary file until complete file is written out to
                    // disk
                    final File tmp = new File(pom.getParentFile(),
                            pom.getName() + ".tmp");
                    // TODO use embedded ruby best via a component to reuse the
                    // state of the spec fetcher
                    execute(new String[] {
                                    "-e",
                                    "ARGV[0] = '"
                                            + artifact.getFile()
                                                    .getAbsolutePath()
                                            + "'\nrequire('"
                                            + fileFromClassloader("spec2pom.rb")
                                            + "')" },
                            pom,
                            false);

                    tmp.renameTo(artifact.getFile());
                    pom.setLastModified(artifact.getFile().lastModified());
                    return true;
                }
            }
        }
        return false;
    }

    private String key(final Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    @SuppressWarnings("unchecked")
    private void collectArtifacts(final Artifact artifact,
            final Map<String, Artifact> visitedArtifacts,
            final boolean includeTest) throws MojoExecutionException {
        getLog().debug("<gems> collect artifacts for " + artifact);
        resolve(artifact);
        try {
            final MavenProject project = artifact != this.project.getArtifact()
                    ? this.builder.buildFromRepository(artifact,
                                                       this.remoteRepositories,
                                                       this.localRepository)
                    : this.project;

            project.setDependencyArtifacts(project.createArtifacts(this.artifactFactory,
                                                                   artifact.getScope(),
                                                                   null));
            createMetadatasForDependencies(project);

            project.setRemoteArtifactRepositories(this.remoteRepositories);

            ArtifactResolutionResult result = null;
            boolean retry = true;
            while (retry) {
                try {
                    retry = false;
                    result = this.resolver.resolveTransitively(project.getDependencyArtifacts(),
                                                               project.getArtifact(),
                                                               this.localRepository,
                                                               this.remoteRepositories,
                                                               this.metadata,
                                                               null);
                }
                catch (final AbstractArtifactResolutionException e) {
                    retry = createMetadata(e.getArtifact());
                    if (!retry) {
                        getLog().error("error resolving "
                                               + project.getArtifact(),
                                       e);
                    }
                }
            }
            project.setArtifacts(result.getArtifacts());
            
            final Set<Artifact> walkArtifacts = ( this.useTransitiveDependencies ? (Set<Artifact>)result.getArtifacts() : (Set<Artifact>) project.getDependencyArtifacts() );
            for (final Artifact dependencyArtifact : walkArtifacts ) {
                if ("gem".equals(dependencyArtifact.getType())) {
                    if (!visitedArtifacts.containsKey(key(dependencyArtifact))) {
                        collectArtifacts(dependencyArtifact,
                                         visitedArtifacts,
                                         false);
                    }
                }
            }

            visitedArtifacts.put(key(artifact), artifact);

        }
        catch (final InvalidDependencyVersionException e) {
            throw new MojoExecutionException("resolve error", e);
        }
        catch (final ProjectBuildingException e) {
            throw new MojoExecutionException("Unable to build project due to an invalid dependency version: "
                    + e.getMessage(),
                    e);
        }
        catch (final InvalidVersionSpecificationException e) {
            throw new MojoExecutionException("resolve error", e);
        }

    }

    @SuppressWarnings("unchecked")
    private void createMetadatasForDependencies(final MavenProject project)
            throws InvalidVersionSpecificationException, MojoExecutionException {
        getLog().debug("<gems> process metadata for " + project.getArtifact()
                + " " + project.getDependencies());
        for (final Dependency dep : (List<Dependency>) project.getDependencies()) {
            if ("gem".equals(dep.getType())
                    && (Artifact.SCOPE_COMPILE + Artifact.SCOPE_RUNTIME).contains(dep.getScope())) {
                final Artifact dependencyArtifact = this.artifactFactory.createDependencyArtifact(dep.getGroupId(),
                                                                                                  dep.getArtifactId(),
                                                                                                  VersionRange.createFromVersionSpec(dep.getVersion()),
                                                                                                  dep.getType(),
                                                                                                  dep.getClassifier(),
                                                                                                  dep.getScope());
                createMetadata(dependencyArtifact);
            }
        }
    }

    private boolean createMetadata(final Artifact dependencyArtifact)
            throws MojoExecutionException {
        final ArtifactRepositoryMetadata repositoryMetadata = new ArtifactRepositoryMetadata(dependencyArtifact);

        // TODO do not assume to have only ONE gem repository
        final ArtifactRepository repository = this.gemRepositories.get(0);
        final File metadataFile = new File(this.localRepository.getBasedir(),
                this.localRepository.pathOfLocalRepositoryMetadata(repositoryMetadata,
                                                                   repository));

        // update them only once a day
        if (System.currentTimeMillis() - metadataFile.lastModified() > ONE_DAY_IN_MILLIS) {
            if (this.offline) {
                getLog().debug("<gems> offline mode - skip updating metadata for "
                        + dependencyArtifact);
                return false;
            }
            else {
                getLog().info("<gems> "
                        + (metadataFile.exists() ? "updating" : "creating")
                        + " metadata for " + dependencyArtifact);

                metadataFile.getParentFile().mkdirs();
                // use temporary file until new file is completely written
                final File tmp = new File(metadataFile.getParentFile(),
                        metadataFile.getName() + ".tmp");
                final String script = "ARGV[0] = '"
                        + dependencyArtifact.getArtifactId() + "'\nrequire('"
                        + fileFromClassloader("metadata.rb") + "')";

                try {
                    execute(new String[] { "-e", script }, tmp, false);
                    tmp.renameTo(metadataFile);
                    // TODO is that needed ?
                    metadataFile.setLastModified(System.currentTimeMillis());
                }
                catch (final MojoExecutionException e) {
                    // retry due to often timeout errors
                    // TODO make the retry on timeout errors only !!!
                    try {
                        execute(new String[] { "-e", script }, tmp, false);
                        tmp.renameTo(metadataFile);
                        // TODO is that needed ?
                        metadataFile.setLastModified(System.currentTimeMillis());
                    }
                    catch (final MojoExecutionException ee) {
                        // TODO maybe it is possible to obey fail-fast and
                        // fail-at-end from the command line switches
                        if (metadataFile.exists()) {
                            // touch metadataFile to prevent further updates
                            // today
                            metadataFile.setLastModified(System.currentTimeMillis());
                            getLog().warn("failed to update metadata for "
                                                  + dependencyArtifact
                                                  + ", use old one",
                                          ee);
                        }
                        else {
                            throw ee;
                        }
                    }
                }
                catch (final RuntimeException e) {
                    // TODO maybe it is possible to obey fail-fast and
                    // fail-at-end from the command line switches
                    if (metadataFile.exists()) {
                        // touch metadataFile to prevent further updates today
                        metadataFile.setLastModified(System.currentTimeMillis());
                        getLog().warn("failed to update metadata for "
                                + dependencyArtifact + ", use old one");
                        getLog().debug("metadata failure", e);
                    }
                    else {
                        throw e;
                    }
                }
                tmp.delete();
                this.updateCheckManager.touch(repositoryMetadata,
                                              repository,
                                              metadataFile);
                return true;
            }
        }
        else {
            return false;
        }
    }

    private void resolve(final Artifact artifact) throws MojoExecutionException {
        if (artifact != null && this.project.getArtifact() != artifact) {
            if (artifact.getFile() == null || !artifact.getFile().exists()) {

                getLog().debug("<gems> resolve " + artifact);

                // final ArtifactResolutionRequest request = new
                // ArtifactResolutionReqquest().setArtifact(artifact)
                // .setLocalRepository(this.localRepository)
                // .setRemoteRepositories(this.project.getRemoteArtifactRepositories());
                try {
                    this.resolver.resolve(artifact,
                                          this.remoteRepositories,
                                          this.localRepository);
                }
                catch (final ArtifactResolutionException e) {
                    throw new MojoExecutionException("resolve error", e);
                }
                catch (final ArtifactNotFoundException e) {
                    throw new MojoExecutionException("resolve error", e);
                }
            }
            createMissingPom(artifact);
        }
    }
}
