package de.saumya.mojo.jruby;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.NoSuchRealmException;

import de.saumya.mojo.ruby.Logger;
import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.ruby.script.ScriptFactory;

/**
 * Base for all JRuby mojos.
 *
 * @requiresProject false
 */
public abstract class AbstractJRubyMojo extends AbstractMojo {

    protected static final String JRUBY_COMPLETE = "jruby-complete";

    protected static final String JRUBY_CORE = "jruby-core";

    protected static final String JRUBY_STDLIB = "jruby-stdlib";

    protected static final String DEFAULT_JRUBY_VERSION = "1.7.11";


    /**
     * common arguments
     * <br/>
     * Command line -Dargs=...
     *
     * @parameter expression="${args}"
     */
    protected String args;

    /**
     * jvm arguments for the java command executing jruby
     * <br/>
     * Command line -Djruby.jvmargs=...
     *
     * @parameter expression="${jruby.jvmargs}"
     */
    protected String jrubyJvmArgs;

    /**
     * switches for the jruby command, like '--1.9'
     * <br/>
     * Command line -Djruby.switches=...
     *
     * @parameter expression="${jruby.switches}"
     */
    protected String jrubySwitches;

    /**
     * environment values passed on to the jruby process. needs jrubyFork true.
     * <br/>
     * @parameter
     */
    protected Map<String, String> env;

    /**
     * if the pom.xml has no runtime dependency to a jruby-complete.jar then
     * this version is used to resolve the jruby-complete dependency from the
     * local/remote maven repository. it overwrites the jruby version from
     * the dependencies if any. i.e. you can easily switch jruby version from the commandline !
     * <br/>
     * default: 1.7.11
     * <br/>
     * Command line -Djruby.version=...
     *
     * @parameter expression="${jruby.version}"
     */
    private String jrubyVersion;

    /**
     * fork the JRuby execution.
     * <br/>
     * Command line -Djruby.fork=...
     *
     * @parameter expression="${jruby.fork}" default-value="true"
     */
    protected boolean jrubyFork;

    /**
     * verbose jruby related output
     * <br/>
     * Command line -Djruby.verbose=...
     *
     * @parameter expression="${jruby.verbose}" default-value="false"
     */
    protected boolean jrubyVerbose;

    /**
     * directory with ruby sources - added to java classpath and ruby loadpath
     * <br/>
     * Command line -Djruby.sourceDirectory=...
     *
     * @parameter expression="${jruby.sourceDirectory}" default-value="src/main/ruby"
     */
    protected File rubySourceDirectory;

    /**
     * directory with ruby sources - added to ruby loadpath only
     * <br/>
     * Command line -Djruby.lib=...
     *
     * @parameter expression="${jruby.lib}" default-value="lib"
     */
    protected File libDirectory;

    /**
     * the launch directory for the JRuby execution.
     * <br/>
     * Command line -Djruby.launchDirectory=...
     *
     * @parameter default-value="${project.basedir}" expression="${jruby.launchDirectory}"
     */
    private File launchDirectory;

    /**
     * reference to maven project for internal use.
     *
     * @parameter expression="${project}"
     * @required
     * @readOnly
     */
    protected MavenProject project;

    /**
     * local repository for internal use.
     *
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * classrealm for internal use.
     *
     * @parameter expression="${dummyExpression}"
     * @readonly
     */
    protected ClassRealm classRealm;

    /** @component */
    protected RepositorySystem repositorySystem;

    protected Logger logger;

    protected ScriptFactory factory;

    private JRubyVersion jRubyVersion;

    protected JRubyVersion getJrubyVersion()
    {
        if (jRubyVersion == null )
        {
            this.jRubyVersion = new JRubyVersion( jrubyVersion == null ? DEFAULT_JRUBY_VERSION : jrubyVersion );
        }
        return jRubyVersion;
    }
    
    private ScriptFactory newScriptFactory() throws MojoExecutionException {
    	ScriptFactory factory = createScriptFactory();
    	if( env != null ){
    		for( Map.Entry<String, String> entry: env.entrySet() ){
    			factory.addEnv( entry.getKey(), entry.getValue() );
    		}
    	}
    	return factory;
    }

	private ScriptFactory createScriptFactory() throws MojoExecutionException {
		try {
		    try
		    {
		        classRealm.getWorld().disposeRealm("jruby-all");
		    }
		    catch( NoSuchRealmException ignore )
		    {
		        // ignore
		    }
			ClassRealm realm = classRealm.getWorld().newRealm("jruby-all");
			for (String path : this.project.getTestClasspathElements()) {
				realm.addConstituent(new File(path).toURI().toURL());
			}
			// check if there is jruby present
			Class<?> clazz = realm.loadClass("org.jruby.runtime.Constants");
			if ( jrubyVerbose ){
				String version = clazz.getField( "VERSION" ).get(clazz).toString();
				getLog().info("found jruby on classpath");
				getLog().info("jruby version   : " + version);
			}
			this.classRealm = realm;
			return newScriptFactory( null );
		} catch (final Exception e) {
		    //TODO debug
		    //e.printStackTrace();
			try {
				return newScriptFactory(resolveJRubyArtifact());
			} catch (final DependencyResolutionRequiredException ee) {
				throw new MojoExecutionException("could not resolve jruby", e);
			}
		}
	}

    protected ScriptFactory newScriptFactory(Artifact artifact) throws MojoExecutionException {
        try {
            final ScriptFactory factory = 
            		artifact == null ? 
                            new ScriptFactory(this.logger,
                                    this.classRealm, 
                                    null,
                                    this.project.getTestClasspathElements(), 
                                    this.jrubyFork):
            			(JRUBY_CORE.equals(artifact.getArtifactId()) ?
                    new ScriptFactory(this.logger,
                            this.classRealm, 
                            artifact.getFile(),
                            resolveJRubyStdlibArtifact(artifact).getFile(),
                            this.project.getTestClasspathElements(), 
                            this.jrubyFork) :
                    new ScriptFactory(this.logger,
                            this.classRealm, 
                            artifact.getFile(),
                            this.project.getTestClasspathElements(), 
                            this.jrubyFork) );

            if(libDirectory != null && libDirectory.exists()){
                if(jrubyVerbose){
                    getLog().info("add to ruby loadpath: " + libDirectory.getAbsolutePath());
                }
                // add it to the load path for all scripts using that factory
                factory.addSwitch("-I", libDirectory.getAbsolutePath());
            }
            if(rubySourceDirectory != null && rubySourceDirectory.exists()){
                if(jrubyVerbose){
                    getLog().info("add to ruby loadpath: " + rubySourceDirectory.getAbsolutePath());
                }
                // add it to the load path for all scripts using that factory
                factory.addSwitch("-I", rubySourceDirectory.getAbsolutePath());
            }
            return factory;
        } catch (final DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("could not resolve jruby", e);
        } catch (final ScriptException e) {
            throw new MojoExecutionException(
                    "could not initialize script factory", e);
        } catch (final IOException e) {
            throw new MojoExecutionException(
                    "could not initialize script factory", e);
        }
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        System.setProperty("jbundle.skip", "true");
        this.logger = new MojoLogger(this.jrubyVerbose, getLog());
        this.factory = newScriptFactory();
        this.factory.addEnv("JBUNDLE_SKIP", "true");
        this.factory.addJvmArgs(this.jrubyJvmArgs);
        this.factory.addSwitches(this.jrubySwitches);

        if(rubySourceDirectory != null && rubySourceDirectory.exists()){
            if(jrubyVerbose){
                getLog().info("add to java classpath: " + rubySourceDirectory.getAbsolutePath());
            }
            // add it to the classpath so java classes can find the ruby files
            Resource resource = new Resource();
            resource.setDirectory(rubySourceDirectory.getAbsolutePath());
            project.getBuild().getResources().add(resource);
        }

        try {

            executeJRuby();

        } catch (final IOException e) {
            throw new MojoExecutionException("error in executing jruby", e);
        } catch (final ScriptException e) {
            throw new MojoExecutionException("error in executing jruby", e);
        }
    }

    abstract protected void executeJRuby() throws MojoExecutionException,
            MojoFailureException, IOException, ScriptException;

    protected File launchDirectory() {
        if (this.launchDirectory == null) {
            this.launchDirectory = this.project.getBasedir();
            if (this.launchDirectory == null || !this.launchDirectory.exists()) {
                this.launchDirectory = new File(System.getProperty("user.dir"));
            }
        }
        return this.launchDirectory;
    }

    protected Artifact resolveJRubyCompleteArtifact(final String version)
            throws DependencyResolutionRequiredException {
        getLog().debug("resolve jruby for version " + version);
        final Artifact artifact = this.repositorySystem.createArtifact(
                "org.jruby", JRUBY_COMPLETE, version, "jar");
        return resolveJRubyArtifact(artifact);
    }

    private Artifact resolveJRubyArtifact(final Artifact artifact)
            throws DependencyResolutionRequiredException {
        final ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        request.setLocalRepository(this.localRepository);
        request.setRemoteRepositories(this.project.getRemoteArtifactRepositories());
        this.repositorySystem.resolve(request);

        if (this.jrubyVerbose) {
            getLog().info("jruby version   : " + artifact.getVersion());
        }
        // set it so other plugins can retrieve the version in use
        this.jrubyVersion = artifact.getVersion();
        return artifact;
    }

    protected Artifact resolveJRubyArtifact() throws DependencyResolutionRequiredException,
            MojoExecutionException {
        if (this.jrubyVersion != null) {
            // preference to command line or property version
            return resolveJRubyCompleteArtifact(this.jrubyVersion);
        } 
        else {
            // then take jruby from the dependencies either jruby-complete or jruby-core
            for (final Dependency artifact : this.project.getDependencies()) {
                if ((artifact.getArtifactId().equals(JRUBY_COMPLETE)
                      ||  artifact.getArtifactId().equals(JRUBY_CORE))
                      // TODO this condition is not needed ?
                            && !artifact.getScope().equals(Artifact.SCOPE_PROVIDED)
                        && !artifact.getScope().equals(Artifact.SCOPE_SYSTEM)) {
                    return resolveJRubyArtifact(this.repositorySystem
                            .createArtifact(artifact.getGroupId(), artifact
                                    .getArtifactId(), artifact.getVersion(),
                                    artifact.getType()));
                }
            }
        }
        // finally fall back on the default version of jruby
        return resolveJRubyCompleteArtifact(DEFAULT_JRUBY_VERSION);
    }

    protected Artifact resolveJRubyStdlibArtifact(Artifact jruby) throws DependencyResolutionRequiredException,
            MojoExecutionException {
        final ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        for (final Dependency artifact : this.project.getDependencies()) {
            if (artifact.getArtifactId().equals(JRUBY_STDLIB)
                    // TODO this condition is not needed ?
                    && !artifact.getScope().equals(Artifact.SCOPE_PROVIDED)
                    && !artifact.getScope().equals(Artifact.SCOPE_SYSTEM)) {
                request.setArtifact(this.repositorySystem
                        .createArtifact(artifact.getGroupId(), artifact
                                .getArtifactId(), artifact.getVersion(),
                                artifact.getType()));
                break;
            }
        }
        if (request.getArtifact() == null){
            request.setResolveTransitively(true);
            request.setArtifact(jruby);
        }
        request.setLocalRepository(this.localRepository);
        request.setRemoteRepositories(this.project.getRemoteArtifactRepositories());
        Set<Artifact> set = this.repositorySystem.resolve(request).getArtifacts();
        for (Artifact a: set){
            if (JRUBY_STDLIB.equals(a.getArtifactId())) {
                return a;
            }
        }
        throw new MojoExecutionException("failed to resolve jruby stdlib artifact");
    }
}
