package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;

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

    /** @component role-hint="zip" */
    protected UnArchiver unzip;
    
    /** @parameter expression="${plugin}" @readonly */
    protected PluginDescriptor  plugin;

    /**
     * flag whether to include open-ssl gem or not
     * <br/>
     * Command line -Dgem.includeOpenSSL=...
     * 
     * @parameter expression="${gem.includeOpenSSL}" default-value="true"
     */
    protected boolean       includeOpenSSL;

    /**
     * flag whether to include all gems to test-resources, i.e. to test-classpath or not
     * <br/>
     * Command line -Dgem.includeRubygemsInTestResources=...
     * 
     * @parameter expression="${gem.includeRubygemsInTestResources}" default-value="true"
     */
    protected boolean       includeRubygemsInTestResources;

    /**
     * flag whether to include all gems to resources, i.e. to classpath or not
     * <br/>
     * Command line -Dgem.includeRubygemsInResources=...
     *
     * @parameter expression="${gem.includeRubygemsInResources}" default-value="false"
     */
    protected boolean       includeRubygemsInResources;

    
    /**
     * flag whether to install rdocs of the used gems or not
     * <br/>
     * Command line -Dgem.installRDoc=...
     * 
     * @parameter expression="${gem.installRDoc}" default-value="false"
     */
    protected boolean         installRDoc;

    /**
     * flag whether to install ri of the used gems or not
     * <br/>
     * Command line -Dgem.installRDoc=...
     * 
     * @parameter expression="${gem.installRI}" default-value="false"
     */
    protected boolean         installRI;

    /**
     * use system gems instead of setting up GemPath/GemHome inside the build directory and ignores any set
     * gemHome and gemPath. you need to have both GEM_HOME and GEM_PATH environment variable set to make it work.
     * <br/>
     * Command line -Dgem.useSystem=...
     *
     * @parameter expression="${gem.useSystem}" default-value="false"
     */
    protected boolean          gemUseSystem;

    /**
     * directory of gem home to use when forking JRuby. default will be ignored
     * when gemUseSystem is true.
     * <br/>
     * Command line -Dgem.home=...
     *
     * @parameter expression="${gem.home}"
     *            default-value="${project.build.directory}/rubygems"
     */
    protected File          gemHome;

    /**
     * directory of JRuby path to use when forking JRuby. default will be ignored
     * when gemUseSystem is true.
     * <br/>
     * Command line -Dgem.path=...
     *
     * @parameter expression="${gem.path}"
     *            default-value="${project.build.directory}/rubygems"
     */
    protected File          gemPath;

    /**
     * directory of JRuby bin path to use when forking JRuby.
     * <br/>
     * Command line -Dgem.binDirectory=...
     *
     * @parameter expression="${gem.binDirectory}"
     */
    protected File          binDirectory;

    /**
     * flag to indicate to setup jruby's native support for C-extensions
     * <br/>
     * Command line -Dgem.supportNative=...
     * 
     * @parameter expression="${gem.supportNative}" default-value="false"
     */
    protected boolean        supportNative;
    
    /** @component */
    protected GemManager    manager;

    protected GemsConfig    gemsConfig;

    protected GemsInstaller gemsInstaller;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException{
        if (this.project.getBasedir() == null) {
            this.gemHome = new File(this.gemHome.getAbsolutePath()
                    .replace("/${project.basedir}/", "/"));
            this.gemPath = new File(this.gemPath.getAbsolutePath()
                    .replace("/${project.basedir}/", "/"));
        }

        this.gemsConfig = new GemsConfig();
        this.gemsConfig.setGemHome(this.gemHome);
        this.gemsConfig.addGemPath(this.gemPath);
        if (this.gemUseSystem && 
                (System.getenv("GEM_HOME") == null || System.getenv( "GEM_PATH") == null) ){
            throw new MojoExecutionException("with gemUseSystem set to true you need to provide" +
            		" GEM_HOME and GEM_PATH environment variables !");
        }
        this.gemsConfig.setSystemInstall(this.gemUseSystem);
        
        this.gemsConfig.setAddRdoc(this.installRDoc);
        this.gemsConfig.setAddRI(this.installRI);
        this.gemsConfig.setBinDirectory(this.binDirectory);
        // this.gemsConfig.setUserInstall(userInstall);
        // this.gemsConfig.setSystemInstall(systemInstall);
        this.gemsConfig.setSkipJRubyOpenSSL( ! (this.includeOpenSSL && getJrubyVersion().needsOpenSSL() ) );

        super.execute();
    }

    @Override
    protected ScriptFactory newScriptFactory(Artifact artifact) throws MojoExecutionException {
        try {
            final GemScriptFactory factory = JRUBY_CORE.equals(artifact.getArtifactId()) ?
                    new GemScriptFactory(this.logger,
                                  this.classRealm, 
                                  artifact.getFile(),
                                  resolveJRubyStdlibArtifact(artifact).getFile(),
                                  this.project.getTestClasspathElements(), 
                                  this.jrubyFork, 
                                  this.gemsConfig) :
                    new GemScriptFactory(this.logger,
                                  this.classRealm, 
                                  artifact.getFile(),
                                  this.project.getTestClasspathElements(), 
                                  this.jrubyFork, 
                                  this.gemsConfig);
                  
            if(supportNative){
                factory.addJvmArgs("-Djruby.home=" + setupNativeSupport().getAbsolutePath());
            }
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

    private File setupNativeSupport() throws MojoExecutionException {
        File target = new File(this.project.getBuild().getDirectory());
        File jrubyDir = new File(target, "jruby-" + getJrubyVersion());
        if (!jrubyDir.exists()){
            Artifact dist = manager.createArtifact("org.jruby",
                                                   "jruby-dist",
                                                   getJrubyVersion().toString(),
                                                   "bin",
                                                   "zip");
            try {
                manager.resolve(dist,
                                localRepository,
                                project.getRemoteArtifactRepositories());
            }
            catch (final GemException e) {
                throw new MojoExecutionException("could not setup jruby distribution for native support",
                        e);
            }
            if (jrubyVerbose) {
                getLog().info("unzip " + dist.getFile());
            }
            target.mkdirs();
            unzip.setSourceFile(dist.getFile());
            unzip.setDestDirectory(target);
            File f = null;
            try {
                unzip.extract();
                f = new File(target, "jruby-" + getJrubyVersion() + "/bin/jruby");
                // use reflection so it compiles with java1.5 as well but does not set executable
                Method m = f.getClass().getMethod("setExecutable", boolean.class);
                m.invoke(f, new Boolean(true));
            }
            catch (ArchiverException e) {
                throw new MojoExecutionException("could unzip jruby distribution for native support",
                        e);
            }
            catch (Exception e) {
                getLog().warn("can not set executable flag: " + f.getAbsolutePath() + " (" + e.getMessage() + ")");
            }
        }
        return jrubyDir;
    }

    @Override
    protected void executeJRuby() throws MojoExecutionException,
        MojoFailureException, IOException, ScriptException {
        
        this.gemsInstaller = new GemsInstaller(this.gemsConfig,
                this.factory,
                this.manager);

        try {
            // install the gem dependencies from the pom
            this.gemsInstaller.installPom(this.project, 
                                          this.localRepository);

            // has the plugin gem dependencies ?
            boolean hasGems = false;
            for(Artifact artifact: plugin.getArtifacts()){
                if (artifact.getType().contains("gem")){
                    hasGems = true;
                    break;
                }
            }
            if (hasGems){
                // install the gems for the plugin
                String base = this.gemsConfig.getGemHome() != null ? 
                        this.gemsConfig.getGemHome().getAbsolutePath() : 
                            (project.getBuild().getDirectory() + "/rubygems");
                File pluginGemHome = new File(base + "-" + plugin.getArtifactId());
                pluginGemHome.mkdirs();
                // use a common bindir, i.e. the one from the configured gemHome
                // remove default by setting it explicitly
                this.gemsConfig.setBinDirectory(this.gemsConfig.getBinDirectory());
                // remember gem_home
                File home = this.gemsConfig.getGemHome();
                // use plugin home for plugin gems
                this.gemsConfig.setGemHome(pluginGemHome);
                this.gemsConfig.addGemPath(pluginGemHome);

                this.gemsInstaller.installGems(this.project,
                                               this.plugin.getArtifacts(), 
                                               this.localRepository, 
                                               getRemoteRepos());

                // reset old gem home again
                this.gemsConfig.setGemHome(home);
            }
        }
        catch (final GemException e) {
            throw new MojoExecutionException("error in installing gems", e);
        }

        if (this.includeRubygemsInTestResources) {
            for (File path : this.gemsConfig.getGemPath()) {
                if (jrubyVerbose) {
                    getLog().info("add gems to test-classpath from: "
                            + path.getAbsolutePath());
                }
                // add it to the classpath so java classes can find the ruby
                // files
                Resource resource = new Resource();
                resource.setDirectory(path.getAbsolutePath());
                resource.addInclude("gems/**");
                resource.addInclude("specifications/**");
                project.getBuild().getTestResources().add(resource);
            }
        }

        if (this.includeRubygemsInResources) {
            for (File path : this.gemsConfig.getGemPath()) {
                if (jrubyVerbose) {
                    getLog().info("add gems to classpath from: "
                            + path.getAbsolutePath());
                }
                // add it to the classpath so java classes can find the ruby files
                Resource resource = new Resource();
                resource.setDirectory(path.getAbsolutePath());
                resource.addInclude("gems/**");
                resource.addInclude("specifications/**");
                project.getBuild().getResources().add(resource);
            }
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


    protected List<ArtifactRepository> getRemoteRepos() {
        List<ArtifactRepository> remotes = new LinkedList<ArtifactRepository>();
        remotes.addAll(project.getPluginArtifactRepositories() );
        remotes.addAll(project.getRemoteArtifactRepositories() );
        return remotes;
    }
}
