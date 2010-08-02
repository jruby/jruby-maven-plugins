package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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

    private static List<String> NO_CLASSPATH = Collections.emptyList();

    /**
     * @parameter expression="${gem.includeOpenSSL}" default-value="true"
     */
    protected boolean           includeOpenSSL;

    /**
     * allow to overwrite the version by explicitly declaring a dependency in
     * the pom. will not check any dependencies on gemspecs level.
     * 
     * @parameter expression="${gem.forceVersion}" default-value="false"
     */
    private boolean             forceVersion;

    /**
     * triggers an update of maven metadata for all gems.
     * 
     * @parameter expression="${gem.update}" default-value="false"
     */
    private boolean             update;

    protected final Log         log          = new Log() {
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
        if (this.artifacts.size() > 0) {
            setupGems(this.artifacts, false);
        }
        executeWithGems();
    }

    protected void setupGems(final Artifact artifact)
            throws MojoExecutionException {
        setupGems(Arrays.asList(new Artifact[] { artifact }), true);
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

    @SuppressWarnings("unchecked")
    private void setupGems(Collection<Artifact> artifacts, final boolean resolve)
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
                collectArtifacts(artifact, collectedArtifacts, resolve);
            }
        }

        collectedArtifacts.remove(key(this.project.getArtifact()));

        String extraFlag = "";
        if (this.forceVersion) {
            // allow to overwrite resolved version with version of project
            // dependencies
            for (final Dependency artifact : (List<Dependency>) this.project.getDependencies()) {
                final Artifact a = collectedArtifacts.get(artifact.getGroupId()
                        + ":" + artifact.getArtifactId());
                if (!a.getVersion().equals(artifact.getVersion())) {
                    extraFlag = "--force";
                    a.setVersion(artifact.getVersion());
                    a.setResolved(false);
                    a.setFile(null);
                    resolve(a);
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
            execute("-S gem install --no-ri --no-rdoc --no-user-install "
                    + extraFlag + " -l " + gems, false);
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
            final Map<String, Artifact> visitedArtifacts, final boolean resolve)
            throws MojoExecutionException {
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

            final List<Artifact> artifacts = new ArrayList<Artifact>();

            try {
                if (resolve) {
                    final ArtifactResolutionResult result = this.resolver.resolveTransitively(project.getDependencyArtifacts(),
                                                                                              project.getArtifact(),
                                                                                              this.project.getManagedVersionMap(),
                                                                                              this.localRepository,
                                                                                              this.remoteRepositories,
                                                                                              this.metadata);
                    project.setArtifacts(result.getArtifacts());
                    for (final Artifact a : (Set<Artifact>) result.getArtifacts()) {
                        artifacts.add(a);
                        this.project.getArtifactMap()
                                .put(a.getGroupId() + ":" + a.getArtifactId(),
                                     a);
                    }
                }
                else {
                    for (final Artifact a : (Set<Artifact>) project.getDependencyArtifacts()) {
                        artifacts.add((Artifact) this.project.getArtifactMap()
                                .get(a.getGroupId() + ":" + a.getArtifactId()));
                    }
                }
            }
            catch (final AbstractArtifactResolutionException e) {
                if (!getLog().isInfoEnabled()) {
                    getLog().debug("error resolving " + project.getArtifact(),
                                   e);
                }
                else {
                    getLog().warn("error resolving " + project.getArtifact()
                                          + "\n\tjust ignored for now . . .",
                                  e);
                }
                project.setArtifacts(Collections.emptySet());
            }

            for (final Artifact dependencyArtifact : artifacts) {
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
        if (artifact.getFile() == null || !artifact.getFile().exists()) {
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
                throw new MojoExecutionException("not found " + e.getArtifact(),
                        e);
            }
        }
    }
}
