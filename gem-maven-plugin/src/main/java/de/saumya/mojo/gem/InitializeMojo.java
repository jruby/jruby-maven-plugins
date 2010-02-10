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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
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

import de.saumya.mojo.jruby.AbstractJRubyMojo;

/**
 * @goal initialize
 * @phase initialize
 */
public class InitializeMojo extends AbstractJRubyMojo {

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

    @Override
    public void execute() throws MojoExecutionException {
        // System.out.append(this.wagonManager.getClass().getName());
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
            getLog().warn("gem plugin configured but no gem repository found");
            return;
        }
        final File gemsDir = new File(this.gemPath, "gems");
        final StringBuilder gems = new StringBuilder();
        final Map<String, Artifact> collectedArtifacts = new LinkedHashMap<String, Artifact>();
        collectArtifacts(this.project.getArtifact(), collectedArtifacts, true);
        collectedArtifacts.remove(key(this.project.getArtifact()));

        // System.out.println(collectedArtifacts.values());
        // System.out.println(this.project.getDependencyArtifacts());
        // System.out.println(this.project.getArtifacts());

        for (final Artifact artifact : collectedArtifacts.values()) {
            if (artifact.getType().contains("gem")) {
                final String prefix = artifact.getGroupId().equals("rubygems")
                        ? ""
                        : artifact.getGroupId() + ".";
                final File gemDir = new File(gemsDir,
                        prefix
                                + (artifact.getFile()
                                        .getName()
                                        .replaceAll(".gem$", "").replace("-SNAPSHOT",
                                                                         "")));
                // TODO force flag to install gems via command line argument
                if (!(this.fork && gemDir.exists())) {
                    gems.append(" ").append(artifact.getFile()
                            .getAbsolutePath());
                }
                else {
                    getLog().info("already installed: " + artifact);
                }
            }
        }
        if (gems.length() > 0) {
            getLog().info(gems.toString());
            execute("-S gem install -l " + gems);
        }
        else {
            getLog().info("no gems found to install");
        }
    }

    private void createMissingPom(final Artifact artifact)
            throws MojoExecutionException {
        final File pom = new File(artifact.getFile()
                .getPath()
                .replaceFirst("(-java)?.gem$", ".pom"));
        if (artifact.getGroupId().equals("rubygems")
                && (pom.lastModified() != artifact.getFile().lastModified() || pom.length() == artifact.getFile()
                        .length())) {
            getLog().info("creating pom for " + artifact);

            execute(new String[] {
                    "-e",
                    "ARGV[0] = '" + artifact.getFile().getAbsolutePath()
                            + "'\nrequire('" + embeddedRubyFile("spec2pom.rb")
                            + "')" }, pom, false);
            pom.setLastModified(artifact.getFile().lastModified());
        }
    }

    private String key(final Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    @SuppressWarnings("unchecked")
    private void collectArtifacts(final Artifact artifact,
            final Map<String, Artifact> visitedArtifacts,
            final boolean includeTest) throws MojoExecutionException {
        getLog().info("collect artifact for " + artifact);
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
                }
            }
            project.setArtifacts(result.getArtifacts());
            for (final Artifact dependencyArtifact : (Set<Artifact>) project.getDependencyArtifacts()) {
                if ("gem".equals(dependencyArtifact.getType())) {
                    if (!visitedArtifacts.containsKey(key(dependencyArtifact))) {
                        collectArtifacts(dependencyArtifact,
                                         visitedArtifacts,
                                         false);
                    }
                }
            }

            // for (final Dependency dep : (List<Dependency>)
            // project.getDependencies()) {
            // if ("gem".equals(dep.getType())) {
            // final Artifact dependencyArtifact =
            // this.artifactFactory.createDependencyArtifact(dep.getGroupId(),
            // dep.getArtifactId(),
            // VersionRange.createFromVersionSpec(dep.getVersion()),
            // dep.getType(),
            // dep.getClassifier(),
            // dep.getScope());
            // if ((Artifact.SCOPE_COMPILE + "+" +
            // Artifact.SCOPE_RUNTIME).contains(dependencyArtifact.getScope())
            // && !visitedArtifacts.containsKey(key(dependencyArtifact))) {
            // if (dependencyArtifact.getVersion() != null) {
            // collectArtifacts(dependencyArtifact,
            // visitedArtifacts,
            // false);
            // }
            // else {
            //
            // }
            // }
            // }
            // }
            visitedArtifacts.put(key(artifact), artifact);

            // if (true) {
            // return;
            // }
            //
            // project.setDependencyArtifacts(project.createArtifacts(this.artifactFactory,
            // artifact.getScope(),
            // null));
            //
            // // final ArtifactResolutionRequest request = new
            // // ArtifactResolutionRequest().setArtifact(project.getArtifact())
            // // .setArtifactDependencies(project.getDependencyArtifacts())
            // // .setLocalRepository(this.localRepository)
            // //
            // .setRemoteRepositories(project.getRemoteArtifactRepositories())
            // // .setManagedVersionMap(project.getManagedVersionMap());
            // // request.setResolveTransitively(true);
            // final List<Artifact> filtered = new ArrayList<Artifact>();
            // final ArtifactFilter filter = new ArtifactFilter() {
            //
            // @Override
            // public boolean include(final Artifact artifact) {
            // if (Artifact.SCOPE_TEST.equals(artifact.getScope())) {
            // filtered.add(artifact);
            // }
            // // System.out.println(artifact
            // // + " "
            // // + artifact.getScope()
            // // + " "
            // // + (includeTest
            // // && Artifact.SCOPE_TEST.equals(artifact.getScope()) ||
            // // !Artifact.SCOPE_TEST.equals(artifact.getScope())));
            // return (includeTest &&
            // Artifact.SCOPE_TEST.equals(artifact.getScope()))
            // || !Artifact.SCOPE_TEST.equals(artifact.getScope());
            // }
            // };
            //
            // // System.out.println(artifact + " --> " +
            // project.getArtifacts());
            // if (artifact.getScope() == null
            // || (includeTest &&
            // Artifact.SCOPE_TEST.equals(artifact.getScope()))
            // || !Artifact.SCOPE_TEST.equals(artifact.getScope())) {
            // final ArtifactResolutionResult result =
            // this.resolver.resolveTransitively(project.getDependencyArtifacts(),
            // project.getArtifact(),
            // this.localRepository,
            // project.getRemoteArtifactRepositories(),
            // this.metadata,
            // filter);
            // project.setArtifacts(result.getArtifacts());
            // // System.out.println(artifact + " --> " +
            // // project.getArtifacts());
            // for (final Artifact depArtifact : project.getArtifacts()) {
            // if ((Artifact.SCOPE_COMPILE + "+" +
            // Artifact.SCOPE_RUNTIME).contains(depArtifact.getScope())
            // && !visitedArtifacts.containsKey(key(depArtifact))) {
            // collectArtifacts(depArtifact, visitedArtifacts, false);
            // }
            // else {
            // if (depArtifact.getFile() != null
            // && depArtifact.getFile().exists()) {
            // createMissingPom(depArtifact);
            // }
            // }
            // }
            // for (final Artifact depArtifact :
            // project.getDependencyArtifacts()) {
            // if (depArtifact.getFile() != null
            // && depArtifact.getFile().exists()) {
            // createMissingPom(depArtifact);
            // }
            // }
            // for (final Artifact depArtifact : filtered) {
            // resolve(depArtifact);
            // }
            //
            // getLog().info("visited " + artifact + " "
            // + result.getArtifacts()
            // + project.getDependencyArtifacts());
            // visitedArtifacts.put(key(artifact), artifact);
            // }
            // else {
            // getLog().info("skipped visit of " + artifact);
            // }
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
        getLog().info("process metadata for " + project.getArtifact() + " "
                + project.getDependencies());
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
            getLog().info("creating metadata for " + dependencyArtifact);

            metadataFile.getParentFile().mkdirs();
            execute(new String[] {
                    "-e",
                    "ARGV[0] = '" + dependencyArtifact.getArtifactId()
                            + "'\nrequire('" + embeddedRubyFile("metadata.rb")
                            + "')" }, metadataFile, false);

            this.updateCheckManager.touch(repositoryMetadata,
                                          repository,
                                          metadataFile);
            return true;
        }
        else {
            return false;
        }
    }

    private void resolve(final Artifact artifact) throws MojoExecutionException {
        if (artifact != null && this.project.getArtifact() != artifact) {
            if (artifact.getFile() == null || !artifact.getFile().exists()) {
                getLog().info("resolve " + artifact);

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
