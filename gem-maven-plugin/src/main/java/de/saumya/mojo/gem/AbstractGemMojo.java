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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

import de.saumya.mojo.gems.GemspecConverter;
import de.saumya.mojo.jruby.AbstractJRubyMojo;
import de.saumya.mojo.ruby.GemException;
import de.saumya.mojo.ruby.GemScriptFactory;
import de.saumya.mojo.ruby.GemService;
import de.saumya.mojo.ruby.GemifyManager;
import de.saumya.mojo.ruby.RubyScriptException;
import de.saumya.mojo.ruby.Script;
import de.saumya.mojo.ruby.ScriptFactory;

/**
 */
public abstract class AbstractGemMojo extends AbstractJRubyMojo {

    private static Set<Artifact> NO_ARTIFACTS = Collections.emptySet();

    /**
     * @parameter expression="${gem.includeOpenSSL}" default-value="true"
     */
    protected boolean            includeOpenSSL;

    /**
     * @parameter expression="${gem.installRDoc}" default-value="false"
     */
    protected boolean            installRDoc;

    /**
     * @parameter expression="${gem.installRI}" default-value="false"
     */
    protected boolean            installRI;

    /**
     * allow to overwrite the version by explicitly declaring a dependency in
     * the pom. will not check any dependencies on gemspecs level.
     * 
     * @parameter expression="${gem.forceVersion}" default-value="false"
     */
    private boolean              forceVersion;

    /**
     * triggers an update of maven metadata for all gems.
     * 
     * @parameter expression="${gem.update}" default-value="false"
     */
    private boolean              update;
    /**
     * directory of gem home to use when forking JRuby.
     * 
     * @parameter expression="${gem.home}"
     *            default-value="${project.build.directory}/rubygems"
     */
    protected File               gemHome;

    /**
     * directory of JRuby path to use when forking JRuby.
     * 
     * @parameter expression="${gem.path}"
     *            default-value="${project.build.directory}/rubygems"
     */
    protected File               gemPath;

    /**
     * arguments for the gem command.
     * 
     * @parameter default-value="${gem.args}"
     */
    protected String             gemArgs;

    /** @component */
    protected GemifyManager      manager;

    protected GemService         gemService;

    @SuppressWarnings("deprecation")
    @Override
    protected ScriptFactory newScriptFactory() throws MojoExecutionException {
        try {
            // give preference to the gemHome/gemPath from super
            if (super.jrubyGemHome != null) {
                this.gemHome = super.jrubyGemHome;
            }
            if (super.jrubyGemPath != null) {
                this.gemPath = super.jrubyGemPath;
            }
            final GemScriptFactory factory = new GemScriptFactory(this.logger,
                    this.classRealm,
                    resolveJRUBYCompleteArtifact().getFile(),
                    this.project.getTestClasspathElements(),
                    this.jrubyFork,
                    this.gemHome,
                    this.gemPath);
            this.gemService = factory;
            return factory;
        }
        catch (final DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("could not resolve jruby", e);
        }
        catch (final RubyScriptException e) {
            throw new MojoExecutionException("could not initialize script factory",
                    e);
        }
        catch (final IOException e) {
            throw new MojoExecutionException("could not initialize script factory",
                    e);
        }
    }

    @Override
    protected void executeJRuby() throws MojoExecutionException,
            MojoFailureException, IOException, RubyScriptException {
        if (this.project.getBasedir() == null) {
            this.gemHome = new File(this.gemHome.getAbsolutePath()
                    .replace("/${project.basedir}/", "/"));
            this.gemPath = new File(this.gemPath.getAbsolutePath()
                    .replace("/${project.basedir}/", "/"));
        }

        updateMetadata();
        if (this.project.getArtifacts().size() > 0) {
            setupGems(this.project.getArtifacts(), false);
        }
        executeWithGems();
    }

    protected void setupGems(final Artifact artifact)
            throws MojoExecutionException, IOException, RubyScriptException {
        setupGems(Arrays.asList(new Artifact[] { artifact }), true);
    }

    void updateMetadata() throws MojoExecutionException {
        if (this.update) {
            final List<String> done = new ArrayList<String>();
            for (final ArtifactRepository repo : this.project.getRemoteArtifactRepositories()) {
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
                final GemspecConverter gemService = new GemspecConverter(this.logger,
                        this.factory);
                final List<String> ids = new ArrayList<String>();
                for (final ArtifactRepository repo : this.project.getRemoteArtifactRepositories()) {
                    ids.add(repo.getId());
                }
                gemService.updateMetadata(ids,
                                          this.localRepository.getBasedir());
            }
            catch (final RubyScriptException e) {
                throw new MojoExecutionException("error in rake script", e);
            }
            catch (final IOException e) {
                throw new MojoExecutionException("IO error", e);
            }
        }
    }

    private void setupGems(Collection<Artifact> artifacts, final boolean resolve)
            throws MojoExecutionException, IOException, RubyScriptException {
        if (this.includeOpenSSL) {
            Artifact openssl;
            try {
                openssl = this.manager.createGemArtifact("jruby-openssl", "0.7");
                artifacts = new HashSet<Artifact>(artifacts);
                artifacts.add(openssl);
            }
            catch (final GemException e) {
                throw new MojoExecutionException("error creating artifact coordinate for jruby-openssl",
                        e);
            }
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

        String extraFlag = null;
        if (this.forceVersion) {
            // allow to overwrite resolved version with version of project
            // dependencies
            for (final Dependency artifact : this.project.getDependencies()) {
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
            final Script script = this.factory.newScriptFromResource(GEM_RUBY_COMMAND)
                    .addArg("install");
            if (this.installRDoc) {
                script.addArg("--rdoc");
            }
            else {
                script.addArg("--no-rdoc");
            }
            if (this.installRI) {
                script.addArg("--ri");
            }
            else {
                script.addArg("--no-ri");
            }
            script.addArg("--no-user-install")
                    .addArg("-l")
                    .addArg(extraFlag)
                    .addArgs(gems.toString())
                    .execute();
        }
        else {
            getLog().debug("no gems found to install");
        }
    }

    abstract protected void executeWithGems() throws MojoExecutionException,
            RubyScriptException, IOException, MojoFailureException;

    private String key(final Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    private void collectArtifacts(final Artifact artifact,
            final Map<String, Artifact> visitedArtifacts, final boolean resolve)
            throws MojoExecutionException {
        getLog().debug("<gems> collect artifacts for " + artifact);
        resolve(artifact);
        try {
            final MavenProject project = artifact != this.project.getArtifact()
                    ? this.builder.buildFromRepository(artifact,
                                                       this.project.getRemoteArtifactRepositories(),
                                                       this.localRepository)
                    : this.project;

            project.setDependencyArtifacts(project.createArtifacts(this.artifactFactory,
                                                                   artifact.getScope(),
                                                                   null));

            project.setRemoteArtifactRepositories(this.project.getRemoteArtifactRepositories());

            final List<Artifact> artifacts = new ArrayList<Artifact>();

            try {
                if (resolve) {
                    final ArtifactResolutionResult result = this.resolver.resolveTransitively(project.getDependencyArtifacts(),
                                                                                              project.getArtifact(),
                                                                                              this.project.getManagedVersionMap(),
                                                                                              this.localRepository,
                                                                                              this.project.getRemoteArtifactRepositories(),
                                                                                              this.metadata);
                    project.setArtifacts(result.getArtifacts());
                    for (final Artifact a : result.getArtifacts()) {
                        artifacts.add(a);
                        this.project.getArtifactMap()
                                .put(a.getGroupId() + ":" + a.getArtifactId(),
                                     a);
                    }
                }
                else {
                    for (final Artifact a : project.getDependencyArtifacts()) {
                        artifacts.add(this.project.getArtifactMap()
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
                project.setArtifacts(NO_ARTIFACTS);
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
                                            this.project.getRemoteArtifactRepositories(),
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
