package de.saumya.mojo.jruby;

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.model.Dependency;
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

    private static String DEFAULT_JRUBY_VERSION = "1.5.2";

    public static final String GEM_RUBY_COMMAND = "META-INF/jruby.home/bin/gem";

    public static final String IRB_RUBY_COMMAND = "jirb";

    public static final String IRB_SWING_RUBY_COMMAND = "jirb_swing";

    public static final String RAKE_RUBY_COMMAND = "META-INF/jruby.home/bin/rake";

    /**
     * common arguments
     * 
     * @parameter expression="${args}"
     */
    protected String args;

    /**
     * arguments for the jruby command.
     * 
     * @parameter default-value="${jruby.args}"
     */
    protected String jrubyArgs = null;

    /**
     * if the pom.xml has no runtime dependency to a jruby-complete.jar then
     * this version is used to resolve the jruby-complete dependency from the
     * local/remote maven repository. defaults to "1.5.2".
     * 
     * @parameter default-value="${jruby.version}"
     */
    protected String jrubyVersion;

    /**
     * fork the JRuby execution.
     * 
     * @parameter expression="${jruby.fork}" default-value="true"
     */
    protected boolean jrubyFork;

    /**
     * verbose jruby related output
     * 
     * @parameter expression="${jruby.verbose}" default-value="false"
     */
    protected boolean jrubyVerbose;

    /**
     * the launch directory for the JRuby execution.
     * 
     * @parameter default-value="${launchDirectory}"
     */
    private File launchDirectory;

    /**
     * directory of gem home to use when forking JRuby.
     * 
     * @parameter expression="${jruby.gem.home}"
     */
    @Deprecated
    protected File jrubyGemHome;

    /**
     * directory of JRuby path to use when forking JRuby.
     * 
     * @parameter expression="${jruby.gem.path}"
     */
    @Deprecated
    protected File jrubyGemPath;

    /**
     * reference to maven project for internal use.
     * 
     * @parameter expression="${project}"
     * @required
     * @readOnly true
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
            final ScriptFactory factory = new ScriptFactory(this.logger,
                    this.classRealm, resolveJRUBYCompleteArtifact().getFile(),
                    this.project.getTestClasspathElements(), this.jrubyFork);
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
        this.logger = new MojoLogger(this.jrubyVerbose, getLog());
        this.factory = newScriptFactory();
        this.factory.addJavaArgs(this.jrubyArgs);
        if (this.jrubyGemHome != null) {
            this.factory.addEnv("GEM_HOME", this.jrubyGemHome.getAbsolutePath()
                    .replaceFirst(".*/[$][{]project.basedir[}]/", ""));
        }
        if (this.jrubyGemPath != null) {
            this.factory.addEnv("GEM_PATH", this.jrubyGemPath.getAbsolutePath()
                    .replaceFirst(".*/[$][{]project.basedir[}]/", ""));
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

    private Artifact resolveJRUBYCompleteArtifact(final String version)
            throws DependencyResolutionRequiredException {
        getLog().debug("resolve jruby for verions " + version);
        final Artifact artifact = this.repositorySystem.createArtifact(
                "org.jruby", "jruby-complete", version, "jar");
        return resolveJRUBYCompleteArtifact(artifact);
    }

    private Artifact resolveJRUBYCompleteArtifact(final Artifact artifact)
            throws DependencyResolutionRequiredException {
        final ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        request.setLocalRepository(this.localRepository);
        request.setRemoteRepositories(this.project
                .getRemoteArtifactRepositories());
        this.repositorySystem.resolve(request);

        if (this.jrubyVerbose) {
            getLog().info("jruby version   : " + artifact.getVersion());
        }
        return artifact;
    }

    protected Artifact resolveJRUBYCompleteArtifact()
            throws DependencyResolutionRequiredException,
            MojoExecutionException {
        if (this.jrubyVersion != null) {
            // preference to command line or property version
            return resolveJRUBYCompleteArtifact(this.jrubyVersion);
        } else {
            // then take jruby from the dependencies
            for (final Object o : this.project.getDependencies()) {
                final Dependency artifact = (Dependency) o;
                if (artifact.getArtifactId().equals("jruby-complete")
                        && !artifact.getScope().equals(Artifact.SCOPE_PROVIDED)
                        && !artifact.getScope().equals(Artifact.SCOPE_SYSTEM)) {
                    return resolveJRUBYCompleteArtifact(this.repositorySystem
                            .createArtifact(artifact.getGroupId(), artifact
                                    .getArtifactId(), artifact.getVersion(),
                                    artifact.getType()));
                }
            }
        }
        // take the default version of jruby
        return resolveJRUBYCompleteArtifact(DEFAULT_JRUBY_VERSION);
    }
}
