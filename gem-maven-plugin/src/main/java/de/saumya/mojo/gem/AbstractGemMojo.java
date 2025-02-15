package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
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

    @Component( hint="zip" )
    protected UnArchiver unzip;

    @Parameter( defaultValue = "${plugin}", readonly = true )
    protected PluginDescriptor  plugin;

    /**
     * flag whether to include all gems to test-resources, i.e. to test-classpath or not
     *
     * Command line -Dgem.includeRubygemsInTestResources=...
     * 
     */
    @Parameter( defaultValue = "true", property = "gem.includeRubygemsInTestResources" )
    protected boolean       includeRubygemsInTestResources;

    /**
     * flag whether to include all gems to resources, i.e. to classpath or not
     *
     * Command line -Dgem.includeRubygemsInResources=...
     *
     */
    @Parameter( defaultValue = "false", property = "gem.includeRubygemsInResources" )
    protected boolean       includeRubygemsInResources;

    /**
     * flag whether to include all gems to resources, i.e. to classpath or not
     *
     * Command line -Dgem.includeProvidedRubygemsInResources=...
     *
     * parameter: expression="${gem.includeProvidedRubygemsInResources}" default-value="false"
     */
    @Parameter( defaultValue = "false", property = "gem.includeProvidedRubygemsInResources" )
    protected boolean       includeProvidedRubygemsInResources;
    
    /**
     * EXPERIMENTAL
     * 
     * this gives the scope of the gems which shall be included to resources.
     * 
     * flag whether to include all gems to resources, i.e. to classpath or not.
     * the difference to the <code>includeRubygemsInResources</code> is that it 
     * does not depend on rubygems during runtime since the required_path of the 
     * gems gets added to resources. note that it expect the required_path of the 
     * gem to be <b>lib</b> which is the default BUT that is not true for all gems.
     * in this sense this feature is incomplete and might not work for you !!!
     * 
     * IMPORTANT: it only adds the gems with <b>provided</b> scope since they are packed
     * with the jar and then the pom.xml will not have them (since they are marked 
     * 'provided') as transitive dependencies.
     * 
     * this feature can be helpful in situations where the classloader does not work
     * for rubygems due to rubygems uses file system globs to find the gems and this
     * only works if the classloader reveals the jar url of its jars (i.e. URLClassLoader).
     * for example OSGi classloader can not work with rubygems !! 
     *
     * Command line -Dgem.includeGemsInResources=...
     *
     */
    @Parameter( property = "gem.includeGemsInResources" )
    @Deprecated
    protected String       includeGemsInResources;

    /**
    /**
     * flag whether to include file under the lib directory
     *
     * Command line -Dgem.includeLibDirectoryInResources=...
     *
     */
    @Parameter( defaultValue = "false", property = "gem.includeLibDirectoryInResources" )
    protected boolean       includeLibDirectoryInResources;

    
    /**
     * flag whether to install rdocs of the used gems or not
     *
     * Command line -Dgem.installRDoc=...
     * 
     */
    @Parameter( defaultValue = "false", property = "gem.installRDoc" )
    protected boolean         installRDoc;

    /**
     * flag whether to install ri of the used gems or not
     *
     * Command line -Dgem.installRDoc=...
     * 
     */
    @Parameter( defaultValue = "false", property = "gem.installRI" )
    protected boolean         installRI;

    /**
     * use system gems instead of setting up GemPath/GemHome inside the build directory and ignores any set
     * gemHome and gemPath. you need to have both GEM_HOME and GEM_PATH environment variable set to make it work.
     *
     * Command line -Dgem.useSystem=...
     *
     */
    @Parameter( defaultValue = "false", property = "gem.useSystem" )
    protected boolean           gemUseSystem;

    /**
     * map different install locations for rubygems (GEM_HOME) to a directory. for example
     * compile dependencies will be installed in ${project.build.directory}/rubygems and
     * provided dependencies in ${project.build.directory}/rubygems-provided, and 
     * ${project.build.directory}/rubygems-test for the test scope. this mapping here allows
     * to map those different directories onto a single one, i.e.: test =&gt; ${gem.home}, provided =&gt; ${gem.home}
     */
    @Parameter( property = "gem.homes" )
    protected Map<String, String> gemHomes;

    /**
     * directory of gem home to use when forking JRuby. default will be ignored
     * when gemUseSystem is true.
     *
     * Command line -Dgem.home=...
     */
    @Parameter( property = "gem.home", defaultValue = "${project.build.directory}/rubygems" )
    protected File               gemHome;

    /**
     * directory of JRuby path to use when forking JRuby. default will be ignored
     * when gemUseSystem is true.
     *
     * Command line -Dgem.path=...
     */
    @Parameter( property = "gem.path", defaultValue = "${project.build.directory}/rubygems" )
    protected File          gemPath;

    /**
     * directory of JRuby bin path to use when forking JRuby.
     *
     * Command line -Dgem.binDirectory=...
     */
    @Parameter( property = "gem.binDirectory" )
    protected File          binDirectory;

    /**
     * flag to indicate to setup jruby's native support for C-extensions
     *
     * Command line -Dgem.supportNative=...
     * 
     * parameter: expression="${gem.supportNative}" default-value="false"
     */
    @Parameter( defaultValue = "false", property = "gem.supportNative" )
    @Deprecated
    protected boolean        supportNative;
    
    @Component
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
        try {
            this.gemsConfig.setGemHome(this.gemHome.getCanonicalFile());
            this.gemsConfig.addGemPath(this.gemPath.getCanonicalFile());
        }
        catch (IOException e) {
            // fallback to the given files
            this.gemsConfig.setGemHome(this.gemHome);
            this.gemsConfig.addGemPath(this.gemPath);
        }
        if (this.gemUseSystem && 
                (System.getenv("GEM_HOME") == null || System.getenv( "GEM_PATH") == null) ){
            getLog().warn("with gemUseSystem set to true and no GEM_HOME and GEM_PATH is set, " +
            		" then some maven goals might not work as expected");
        }
        this.gemsConfig.setSystemInstall(this.gemUseSystem);
        
        this.gemsConfig.setAddRdoc(this.installRDoc);
        this.gemsConfig.setAddRI(this.installRI);
        this.gemsConfig.setBinDirectory(this.binDirectory);
        // this.gemsConfig.setUserInstall(userInstall);
        // this.gemsConfig.setSystemInstall(systemInstall);

        super.execute();
    }

    @Override
    protected ScriptFactory newScriptFactory(Artifact artifact) throws MojoExecutionException {
        try {
            final GemScriptFactory factory = 
                    artifact == null ? 
                            new GemScriptFactory(this.logger,
                                    this.classRealm, 
                                    null,
                                    getProjectClasspath(), 
                                    this.jrubyFork,
                                    this.gemsConfig):
                        (JRUBY_CORE.equals(artifact.getArtifactId()) ?
                    new GemScriptFactory(this.logger,
                                  this.classRealm, 
                                  artifact.getFile(),
                                  resolveJRubyStdlibArtifact(artifact).getFile(),
                                  getProjectClasspath(), 
                                  this.jrubyFork, 
                                  this.gemsConfig) :
                    new GemScriptFactory(this.logger,
                                  this.classRealm, 
                                  artifact.getFile(),
                                  getProjectClasspath(), 
                                  this.jrubyFork, 
                                  this.gemsConfig));
                  
            if(supportNative){
                factory.addJvmArgs("-Djruby.home=" + setupNativeSupport().getAbsolutePath());
            }
            if(rubySourceDirectory != null && rubySourceDirectory.exists()){
                if(jrubyVerbose){
                    getLog().info("add to ruby loadpath: " + rubySourceDirectory.getAbsolutePath());
                }
                // add it to the load path for all scripts using that factory
                factory.addSwitch("-I", rubySourceDirectory.getAbsolutePath());
            }
            if( libDirectory != null && libDirectory.exists() ){
                if(jrubyVerbose){
                    getLog().info("add to ruby loadpath: " + libDirectory.getAbsolutePath());
                }
                // add it to the load path for all scripts using that factory
                factory.addSwitch("-I", libDirectory.getAbsolutePath());
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

    protected File gemHome( String base, String key )
    {
       if (gemHomes != null && gemHomes.containsKey(key)) {
           return new File(gemHomes.get(key));
       }
       else {
           return new File(base + "-" + key);
       }
    }

    @Override
    protected void executeJRuby() throws MojoExecutionException,
        MojoFailureException, IOException, ScriptException {
        
        this.gemsInstaller = new GemsInstaller(this.gemsConfig,
                this.factory,
                this.manager);

        // remember gem_home
        File home = this.gemsConfig.getGemHome();
        // use a common bindir, i.e. the one from the configured gemHome
        // remove default by setting it explicitly
        this.gemsConfig.setBinDirectory(this.gemsConfig.getBinDirectory());

        // use gemHome as base for other gems installation directories
        String base = this.gemsConfig.getGemHome() != null ? 
                this.gemsConfig.getGemHome().getAbsolutePath() : 
                    (project.getBuild().getDirectory() + "/rubygems");

        try {
            // install the gem dependencies from the pom
            if ( jrubyVerbose )
            {
                getLog().info("installing gems for compile scope . . .");
            }
            this.gemsInstaller.installPom(this.project, 
                                          this.localRepository, "compile");

            if ( jrubyVerbose )
            {
                getLog().info("installing gems for runtime scope . . .");
            }
            this.gemsInstaller.installPom(this.project, 
                                          this.localRepository, "runtime");
            String[] SCOPES = new String[] { "provided", "test" };
            for( String scope: SCOPES ){
                File gemHome = gemHome( base, scope );
                this.gemsConfig.setGemHome( gemHome );
                this.gemsConfig.addGemPath( gemHome );
                
                if ( jrubyVerbose )
                {
                    getLog().info("installing gems for " + scope + " scope . . .");
                }
                // install the gem dependencies from the pom
                this.gemsInstaller.installPom(this.project, 
                                              this.localRepository, 
                                              scope);
               
            }
 
            File pluginGemHome = gemHome( base, plugin.getArtifactId() );
            // use plugin home for plugin gems
            this.gemsConfig.setGemHome(pluginGemHome);
            this.gemsConfig.addGemPath(pluginGemHome);

            if ( jrubyVerbose )
            {
                getLog().info("installing gems for plugin " + plugin.getGroupId() + ":" + plugin.getArtifactId() 
                              + " . . .");
            }
            this.gemsInstaller.installGems(this.project,
                                           this.plugin.getArtifacts(), 
                                           this.localRepository, 
                                           getRemoteRepos());
        }
        catch (final GemException e) {
            throw new MojoExecutionException("error in installing gems", e);
        }
        finally
        {
            // reset old gem home again
            this.gemsConfig.setGemHome(home);
        }

        if (this.includeRubygemsInTestResources) {
            for (File path : this.gemsConfig.getGemPath()) {
                if ( path.exists() ) {
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
                    addResource(project.getBuild().getTestResources(), resource);
                }
            }
        }

        if (this.includeProvidedRubygemsInResources) {
            for (File path : this.gemsConfig.getGemPath()) {
                if ( path.exists() && path.getName().contains("provided")) {
                    if (jrubyVerbose) {
                        getLog().info("add gems to classpath from: "
                                + path.getAbsolutePath());
                    }
                    // add it to the classpath so java classes can find the ruby
                    // files
                    Resource resource = new Resource();
                    resource.setDirectory(path.getAbsolutePath());
                    resource.addInclude("gems/**");
                    resource.addInclude("specifications/**");
                    addResource(project.getBuild().getResources(), resource);
                }
            }
        }
        if (this.includeRubygemsInResources) {
            if (jrubyVerbose) {
                getLog().info("add gems to classpath from: "
                        + home.getAbsolutePath());
            }
            // add it to the classpath so java classes can find the ruby files
            Resource resource = new Resource();
            resource.setDirectory(home.getAbsolutePath());
            resource.addInclude("gems/**");
            resource.addInclude("specifications/**");
            // no java sources since resins application server tries to compile those
            resource.addExclude("gems/**/*.java");
            addResource(project.getBuild().getResources(), resource);
        }

        if (this.includeLibDirectoryInResources) {
            if (jrubyVerbose) {
                getLog().info("add to classpath: "
                        + libDirectory.getAbsolutePath());
            }
            // add it to the classpath so java classes can find the ruby files
            Resource resource = new Resource();
            resource.setDirectory(libDirectory.getAbsolutePath());
            addResource(project.getBuild().getResources(), resource);
        }
        if (this.includeGemsInResources != null ) {
            String dir = "compile".equals( includeGemsInResources ) ? base : base + "-" + includeGemsInResources;
            File gems = new File(dir, "gems");
            if ( gems.exists() )
            {
                for( File g : gems.listFiles() ) {
                    File lib = new File(g, "lib");
                    if (jrubyVerbose) {
                        getLog().info("add to resource: "
                                + lib.getAbsolutePath());
                    }
                    Resource resource = new Resource();
                    resource.setDirectory(lib.getAbsolutePath());
                    project.getBuild().getResources().add(resource);
                }
            }
        }   
         
        try {

            executeWithGems();

        }
        catch (final GemException e) {
            throw new MojoExecutionException("error in executing with gems", e);
        }
    }

    protected void addResource(List<Resource> resources, Resource resource) {
        String ref = resource.toString();
        for( Resource r : resources ) {
            if (r.toString().equals(ref)) {
                return;
            }
        }
        if (jrubyVerbose) {
            logger.info("add resource: " + resource);
        }
        resources.add(resource);
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
