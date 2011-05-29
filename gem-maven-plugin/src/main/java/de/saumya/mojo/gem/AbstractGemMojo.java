package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;

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

    /** @parameter expression="${plugin}" @readonly */
    protected PluginDescriptor  plugin;

    /**
     * @parameter expression="${gem.includeOpenSSL}" default-value="true"
     */
    protected boolean       includeOpenSSL;

    /**
     * @parameter expression="${gem.installRDoc}" default-value="false"
     */
    protected boolean         installRDoc;

    /**
     * @parameter expression="${gem.installRI}" default-value="false"
     */
    protected boolean         installRI;

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

    /**
     * directory of JRuby bin path to use when forking JRuby.
     *
     * @parameter expression="${gem.binDirectory}"
     */
    protected File          binDirectory;

    /** @component */
    protected GemManager    manager;

    protected GemsConfig    gemsConfig;

    protected GemsInstaller gemsInstaller;

    @Override
    protected ScriptFactory newScriptFactory(Artifact artifact) throws MojoExecutionException {
        if (this.project.getBasedir() == null) {
            this.gemHome = new File(this.gemHome.getAbsolutePath()
                    .replace("/${project.basedir}/", "/"));
            this.gemPath = new File(this.gemPath.getAbsolutePath()
                    .replace("/${project.basedir}/", "/"));
        }

        this.gemsConfig = new GemsConfig();
        this.gemsConfig.setGemHome(this.gemHome);
        this.gemsConfig.addGemPath(this.gemPath);

        try {
            final GemScriptFactory factory = new GemScriptFactory(this.logger,
                    this.classRealm,
                    artifact.getFile(),
                    this.project.getTestClasspathElements(),
                    this.jrubyFork,
                    this.gemsConfig);
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
                this.gemsConfig.setAddRdoc(this.installRDoc);
        this.gemsConfig.setAddRI(this.installRI);
        this.gemsConfig.setBinDirectory(this.binDirectory);
        // this.gemsConfig.setUserInstall(userInstall);
        // this.gemsConfig.setSystemInstall(systemInstall);
        this.gemsConfig.setSkipJRubyOpenSSL(!this.includeOpenSSL);

        this.gemsInstaller = new GemsInstaller(this.gemsConfig,
                this.factory,
                this.manager);

        try {
            // install the gem dependecies from the pom
            this.gemsInstaller.installPom(this.project, this.localRepository);

            // has the plugin gem dependencies ?
            boolean hasGems = false;
            for(Artifact artifact: plugin.getArtifacts()){
                if (artifact.getType().contains("gem")){
                    hasGems = true;
                    break;
                }
            }
            // install the gems for the plugin
            if (hasGems){
                // use a common bindir, i.e. the one from the configured gemHome
                // remove default by setting it explicitly
                this.gemsConfig.setBinDirectory(this.gemsConfig.getBinDirectory());
                File home = this.gemsConfig.getGemHome();
                this.gemsConfig.setGemHome(new File(this.gemsConfig.getGemHome().getAbsolutePath() + "-" + plugin.getArtifactId()));
                this.gemsConfig.addGemPath(this.gemsConfig.getGemHome());

                this.gemsInstaller.installGems(this.project, this.plugin.getArtifacts(), this.localRepository);

                this.gemsConfig.setGemHome(home);
            }
        }
        catch (final GemException e) {
            throw new MojoExecutionException("error in installing gems", e);
        }

        // add the gems to the test-classpath
        for(File path: this.gemsConfig.getGemPath()){
            Resource resource = new Resource();
            resource.setDirectory(path.getAbsolutePath());
            project.getBuild().getTestResources().add(resource);
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

}
