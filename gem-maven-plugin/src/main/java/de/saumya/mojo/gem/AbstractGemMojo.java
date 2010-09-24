package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.saumya.mojo.jruby.AbstractJRubyMojo;
import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.gems.GemManager;
import de.saumya.mojo.ruby.gems.GemsConfig;
import de.saumya.mojo.ruby.gems.GemsInstaller;
import de.saumya.mojo.ruby.script.GemScriptFactory;
import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.ruby.script.ScriptFactory;

/**
 */
public abstract class AbstractGemMojo extends AbstractJRubyMojo {

    // private static Set<Artifact> NO_ARTIFACTS = Collections.emptySet();

    /**
     * @parameter expression="${gem.includeOpenSSL}" default-value="true"
     */
    protected boolean       includeOpenSSL;

    /**
     * @parameter expression="${gem.installRDoc}" default-value="false"
     */
    private boolean         installRDoc;

    /**
     * @parameter expression="${gem.installRI}" default-value="false"
     */
    private boolean         installRI;

    // /**
    // * allow to overwrite the version by explicitly declaring a dependency in
    // * the pom. will not check any dependencies on gemspecs level.
    // *
    // * @parameter expression="${gem.forceVersion}" default-value="false"
    // */
    // private boolean forceVersion;

    /**
     * triggers an update of maven metadata for all gems.
     * 
     * @parameter expression="${gem.update}" default-value="false"
     */
    private boolean         update;
    /**
     * directory of gem home to use when forking JRuby.
     * 
     * @parameter expression="${gem.home}"
     *            default-value="${project.build.directory}/rubygems"
     */
    protected File          gemHome;

    /**
     * directory of JRuby path to use when forking JRuby.
     * 
     * @parameter expression="${gem.path}"
     *            default-value="${project.build.directory}/rubygems"
     */
    protected File          gemPath;

    /**
     * arguments for the gem command.
     * 
     * @parameter default-value="${gem.args}"
     */
    protected String        gemArgs;

    // /**
    // * arguments for the gem command during base initialization.
    // *
    // * @parameter default-value="${gem.initializeArgs}"
    // */
    // private String gemInitializeArgs;

    /** @component */
    protected GemManager    manager;

    protected GemsConfig    gemsConfig;

    protected GemsInstaller gemsInstaller;

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
            return factory;
        }
        catch (final DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("could not resolve jruby", e);
        }
        catch (final ScriptException e) {
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
            MojoFailureException, IOException, ScriptException {
        if (this.project.getBasedir() == null) {
            this.gemHome = new File(this.gemHome.getAbsolutePath()
                    .replace("/${project.basedir}/", "/"));
            this.gemPath = new File(this.gemPath.getAbsolutePath()
                    .replace("/${project.basedir}/", "/"));
        }

        this.gemsConfig = new GemsConfig();
        this.gemsConfig.setAddRdoc(this.installRDoc);
        this.gemsConfig.setAddRI(this.installRI);
        this.gemsConfig.setGemHome(this.gemHome);
        this.gemsConfig.setGemPath(this.gemPath);
        // this.gemsConfig.setUserInstall(userInstall);
        this.gemsConfig.setSkipJRubyOpenSSL(!this.includeOpenSSL);

        this.gemsInstaller = new GemsInstaller(this.gemsConfig,
                this.factory,
                this.manager);

        updateMetadata();

        try {
            this.gemsInstaller.installGems(this.project, this.localRepository);
        }
        catch (final GemException e) {
            throw new MojoExecutionException("error in installing gems", e);
        }
        try {

            executeWithGems();

        }
        catch (final GemException e) {
            throw new MojoExecutionException("error in executing with gems", e);
        }
    }

    abstract protected void executeWithGems() throws MojoExecutionException,
            ScriptException, GemException, IOException, MojoFailureException;

    protected void setupGems(final Artifact artifact) throws IOException,
            ScriptException, GemException {
        this.gemsInstaller.installGems(artifact, this.localRepository);
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
}
