package de.saumya.mojo.jruby;

import java.io.File;
import java.io.IOException;

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

    protected static final Object JRUBY_STDLIB = "jruby-stdlib";

    protected static String DEFAULT_JRUBY_VERSION = "1.6.2";


    /**
     * common arguments
     * <br/>
     * Command line -Dargs=...
     *
     * @parameter expression="${args}"
     */
    protected String args;

    /**
     * arguments for the jruby command.
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
     * if the pom.xml has no runtime dependency to a jruby-complete.jar then
     * this version is used to resolve the jruby-complete dependency from the
     * local/remote maven repository. it overwrites the jruby version from
     * the dependencies if any. i.e. you can easily switch jruby version from the commandline !
     * <br/>
     * default: 1.6.1
     * <br/>
     * Command line -Djruby.version=...
     *
     * @parameter expression="${jruby.version}"
     */
    protected String jrubyVersion;

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
     * the launch directory for the JRuby execution.
     * <br/>
     * Command line -Djruby.sourceDirectory=...
     *
     * @parameter expression="${jruby.sourceDirectory}" default-value="src/main/ruby"
     */
    protected File rubySourceDirectory;

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

    protected ScriptFactory newScriptFactory() throws MojoExecutionException {
        try {
            return newScriptFactory(resolveJRubyArtifact());
        }
        catch (final DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("could not resolve jruby", e);
        }
    }

    protected ScriptFactory newScriptFactory(Artifact artifact) throws MojoExecutionException {
        try {
            final ScriptFactory factory = new ScriptFactory(this.logger,
                    this.classRealm, 
                    artifact.getArtifactId().equals(JRUBY_CORE)? null: artifact.getFile(),
                    artifact.getArtifactId().equals(JRUBY_CORE)? retrieveStdlibArtifact().getFile(): artifact.getFile(),
                    this.project.getTestClasspathElements(), 
                    this.jrubyFork);
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

    protected Artifact retrieveStdlibArtifact() throws DependencyResolutionRequiredException {
        for (final Dependency artifact : this.project.getDependencies()) {
            if (artifact.getArtifactId().equals(JRUBY_STDLIB)) {
                return resolveJRubyArtifact(this.repositorySystem
                        .createArtifact(artifact.getGroupId(), artifact
                                .getArtifactId(), artifact.getVersion(),
                                artifact.getType()));
            }
        }
        return null;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        if(rubySourceDirectory.exists()){
            Resource resource = new Resource();
            resource.setDirectory(rubySourceDirectory.getAbsolutePath());
            project.getBuild().getResources().add(resource);
        }

        this.logger = new MojoLogger(this.jrubyVerbose, getLog());
        this.factory = newScriptFactory();
        this.factory.addJvmArgs(this.jrubyJvmArgs);
        this.factory.addSwitches(this.jrubySwitches);

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
}
