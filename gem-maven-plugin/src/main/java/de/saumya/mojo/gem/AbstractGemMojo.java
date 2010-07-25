package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

import de.saumya.mojo.GemService;
import de.saumya.mojo.LauncherFactory;
import de.saumya.mojo.Log;
import de.saumya.mojo.RubyScriptException;
import de.saumya.mojo.jruby.AbstractJRubyMojo;

/**
 */
public abstract class AbstractGemMojo extends AbstractJRubyMojo {

    private static List<String>        NO_CLASSPATH = Collections.emptyList();

    /**
     * @parameter expression="${gem.includeOpenSSL}" default-value="true"
     */
    protected boolean                  includeOpenSSL;

    /**
     * allow to overwrite the version by explicitly declaring a dependency in
     * the pom. will not check any dependencies on gemspecs level.
     * 
     * @parameter expression="${gem.forceVersion}" default-value="false"
     */
    private boolean                    forceVersion;

    /**
     * triggers an update of maven metadata for all gems.
     * 
     * @parameter expression="${gem.update}" default-value="false"
     */
    private boolean                    update;

    /**
     * follow transitive dependencies when initializing rubygem dependencies.
     * 
     * @parameter expression="${gem.useTransitiveDependencies}"
     *            default-value="false"
     */
    boolean                            useTransitiveDependencies;

    /**
     * @parameter default-value="${plugin.artifacts}"
     */
    protected java.util.List<Artifact> pluginArtifacts;

    protected final Log                log          = new Log() {
                                                        public void info(
                                                                final CharSequence content) {
                                                            getLog().info(content);
                                                        }
                                                    };

    public void execute() throws MojoExecutionException {
        if (this.project.getBasedir() == null) {
            this.gemHome = new File(this.gemHome.getAbsolutePath()
                    .replace("/${project.basedir}/", "/"));
            this.gemPath = new File(this.gemPath.getAbsolutePath()
                    .replace("/${project.basedir}/", "/"));
        }

        updateMetadata();
        setupGems(this.artifacts);
        setupGems(this.pluginArtifacts);
        executeWithGems();
    }

    void updateMetadata() throws MojoExecutionException {
        if (this.update) {
            final List<String> done = new ArrayList<String>();
            for (final ArtifactRepository repo : this.remoteRepositories) {
                if (repo.getId().startsWith("rubygems")) {
                    URL url = null;
                    try {
                        url = new URL(repo.getUrl() + "/update");
                        if (!done.contains(url.getHost())) {
                            done.add(url.getHost());
                            final InputStream in = url.openStream();
                            in.read();
                            in.close();
                        }
                    }
                    catch (final IOException e) {
                        throw new MojoExecutionException("error in sending update url: "
                                + url,
                                e);
                    }
                }
            }
        }
    }

    void updateLocalMetadata() throws MojoExecutionException {
        if (this.update) {
            try {
                final GemService gemService = new GemService(this.log,
                        new LauncherFactory().getEmbeddedLauncher(this.verbose,
                                                                  NO_CLASSPATH,
                                                                  setupEnv(),
                                                                  resolveJRUBYCompleteArtifact().getFile(),
                                                                  this.classRealm));
                final List<String> ids = new ArrayList<String>();
                for (final ArtifactRepository repo : this.remoteRepositories) {
                    ids.add(repo.getId());
                }
                gemService.updateMetadata(ids,
                                          this.localRepository.getBasedir());
            }
            catch (final RubyScriptException e) {
                throw new MojoExecutionException("error in rake script", e);
            }
            catch (final DependencyResolutionRequiredException e) {
                throw new MojoExecutionException("could not resolve jruby", e);
            }
            catch (final IOException e) {
                throw new MojoExecutionException("IO error", e);
            }
        }
    }

    protected void setupGems(Collection<Artifact> artifacts)
            throws MojoExecutionException {
        if (this.includeOpenSSL) {
            final Artifact openssl = this.artifactFactory.createArtifact("rubygems",
                                                                         "jruby-openssl",
                                                                         "0.7",
                                                                         "runtime",
                                                                         "gem");
            artifacts = new HashSet<Artifact>(artifacts);
            artifacts.add(openssl);
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
                final File javaGemDir = new File(gemsDir,
                        prefix
                                + (collectedArtifact.getFile()
                                        .getName()
                                        .replaceAll(".gem$", "-java").replace("-SNAPSHOT",
                                                                              "")));
                // TODO force flag to install gems via command line
                // argument
                if (!gemDir.exists() && !javaGemDir.exists()) {
                    gems.append(" ").append(collectedArtifact.getFile()
                            .getAbsolutePath());
                }
                else {
                    getLog().debug("already installed: " + collectedArtifact);
                }
            }
        }
        if (gems.length() > 0) {
            execute("-S gem install --no-ri --no-rdoc " + extraFlag + " "
                    + gems, false);
        }
        else {
            getLog().debug("no gems found to install");
        }
    }

    abstract protected void executeWithGems() throws MojoExecutionException;

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

            project.setRemoteArtifactRepositories(this.remoteRepositories);

            ArtifactResolutionResult result = null;
            try {
                result = this.resolver.resolveTransitively(project.getDependencyArtifacts(),
                                                           project.getArtifact(),
                                                           this.project.getManagedVersionMap(),
                                                           this.localRepository,
                                                           this.remoteRepositories,
                                                           this.metadata,
                                                           null);
                project.setArtifacts(result.getArtifacts());
            }
            catch (final AbstractArtifactResolutionException e) {
                if (!getLog().isInfoEnabled()) {
                    getLog().debug("error resolving " + project.getArtifact(),
                                   e);
                }
                else {
                    getLog().warn("error resolving " + project.getArtifact()
                            + "\n\tjust ignored and let 'gem install' decide !");
                }
                project.setArtifacts(Collections.emptySet());
            }

            final Set<Artifact> walkArtifacts = (this.useTransitiveDependencies
                    ? (Set<Artifact>) result.getArtifacts()
                    : (Set<Artifact>) project.getDependencyArtifacts());
            for (final Artifact dependencyArtifact : walkArtifacts) {
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
    }

    private void resolve(final Artifact artifact) throws MojoExecutionException {
        if (artifact != null && this.project.getArtifact() != artifact) {
            if (artifact.getFile() == null || !artifact.getFile().exists()) {

                try {
                    getLog().debug("<gems> resolve " + artifact
                            + " selected version "
                            + artifact.getSelectedVersion());
                }
                catch (final OverConstrainedVersionException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                if (artifact.getVersion() != "das") {
                    try {
                        this.resolver.resolveAlways(artifact,
                                                    this.remoteRepositories,
                                                    this.localRepository);
                    }
                    catch (final ArtifactResolutionException e) {
                        throw new MojoExecutionException("resolve error "
                                + e.getArtifact(), e);
                    }
                    catch (final ArtifactNotFoundException e) {
                        throw new MojoExecutionException("not found "
                                + e.getArtifact(), e);
                    }
                }
                else {
                    // TODO
                }
                // catch (final NullPointerException e) {
                // throw new MojoExecutionException("npe " + artifact + " "
                // + artifact.getVersion() + " "
                // + artifact.getVersionRange(), e);
                // }
            }
        }
    }
}
