package de.saumya.mojo.jruby;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.classworlds.ClassRealm;

/**
 * Base for all JRuby mojos.
 * 
 * @requiresDependencyResolution compile
 * @requiresProject false
 */
public abstract class AbstractJRubyMojo extends AbstractMojo {

    private static String              DEFAULT_JRUBY_VERSION = "1.4.0";

    private static final Set<Artifact> NO_ARTIFACTS          = Collections.emptySet();

    /**
     * fork the JRuby execution.
     * 
     * @parameter expression="${jruby.fork}" default-value="true"
     */
    protected boolean                  fork;

    /**
     * the launch directory for the JRuby execution.
     * 
     * @parameter expression="${project.basedir}"
     */
    protected File                     launchDirectory;

    /**
     * directory of JRuby home to use when forking JRuby.
     * 
     * @parameter default-value="${jruby.home}"
     */
    protected File                     jrubyHome;

    /**
     * directory of gem home to use when forking JRuby.
     * 
     * @parameter default-value="${jruby.gem.home}"
     */
    protected File                     gemHome;

    /**
     * directory of JRuby path to use when forking JRuby.
     * 
     * @parameter default-value="${jruby.gem.path}"
     */
    protected File                     gemPath;

    /**
     * The amount of memory to use when forking JRuby.
     * 
     * @parameter expression="${jruby.launch.memory}" default-value="384m"
     */
    protected String                   jrubyLaunchMemory;

    /**
     * reference to maven project for internal use.
     * 
     * @parameter expression="${project}"
     * @required
     * @readOnly true
     */
    protected MavenProject             mavenProject;

    /**
     * The project's artifacts.
     * 
     * @parameter default-value="${project.artifacts}"
     * @required
     * @readonly
     */
    protected Set<Artifact>            artifacts;

    /**
     * artifact factory for internal use.
     * 
     * @component
     * @required
     * @readonly
     */
    private ArtifactFactory            artifactFactory;

    /**
     * artifact resolver for internal use.
     * 
     * @component
     * @required
     * @readonly
     */
    protected ArtifactResolver         resolver;

    /**
     * local repository for internal use.
     * 
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository       localRepository;

    /**
     * list of remote repositories for internal use.
     * 
     * @parameter default-value="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    protected List<Repository>         remoteRepositories;

    /**
     * if the pom.xml has no runtime dependency to a jruby-complete.jar then
     * this version is used to resolve the jruby-complete dependency from the
     * local/remote maven repository. defaults to "1.4.0".
     * 
     * @parameter default-value="${jruby.version}"
     */
    protected String                   jrubyVersion;

    /**
     * directory to leave some flags for already installed gems.
     * 
     * @parameter expression="${jruby.gem.flags}"
     *            default-value="${project.build.directory}/gems"
     */
    private File                       gemFlagsDirectory;

    /**
     * output directory for internal use.
     * 
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    protected File                     outputDirectory;

    /**
     * classrealm for internal use.
     * 
     * @parameter expression="${dummyExpression}"
     * @readonly
     */
    private ClassRealm                 classRealm;

    private Artifact resolveJRUBYCompleteArtifact(final String version)
            throws DependencyResolutionRequiredException {
        final Artifact artifact = this.artifactFactory.createArtifactWithClassifier("org.jruby",
                                                                                    "jruby-complete",
                                                                                    version,
                                                                                    "jar",
                                                                                    null);
        try {
            this.resolver.resolve(artifact,
                                  this.remoteRepositories,
                                  this.localRepository);
        }
        catch (final ArtifactResolutionException e) {
            throw new DependencyResolutionRequiredException(artifact);
        }
        catch (final ArtifactNotFoundException e) {
            throw new DependencyResolutionRequiredException(artifact);
        }

        getLog().info("jruby version   : " + version);
        return artifact;
    }

    private Artifact resolveJRUBYCompleteArtifact()
            throws DependencyResolutionRequiredException,
            MojoExecutionException {
        for (final Object o : this.mavenProject.getDependencyArtifacts()) {
            final Artifact artifact = (Artifact) o;
            if (artifact.getArtifactId().equals("jruby-complete")
                    && !artifact.getScope().equals(Artifact.SCOPE_PROVIDED)
                    && !artifact.getScope().equals(Artifact.SCOPE_SYSTEM)) {
                if (this.jrubyVersion != null) {
                    getLog().warn("the configured jruby-version gets ignored in preference to the jruby dependency");
                }
                getLog().info("jruby version   : " + artifact.getVersion());
                return artifact;
            }
        }
        return resolveJRUBYCompleteArtifact(this.jrubyVersion == null
                ? DEFAULT_JRUBY_VERSION
                : this.jrubyVersion);
    }

    protected File launchDirectory() {
        if (this.launchDirectory == null) {
            return new File(System.getProperty("user.dir"));
        }
        else {
            this.launchDirectory.mkdirs();
            return this.launchDirectory;
        }
    }

    protected void ensureGems(final String[] gemNames)
            throws MojoExecutionException {
        final StringBuilder gems = new StringBuilder();
        this.gemFlagsDirectory.mkdirs();
        for (final String gemName : gemNames) {
            if (!new File(this.gemFlagsDirectory, gemName).exists()) {
                gems.append(" ").append(gemName);
            }
        }
        if (gems.length() > 0) {

            execute(("-S maybe_install_gems" + gems.toString()).split("\\s+"),
                    NO_ARTIFACTS);

            for (final String gem : gemNames) {
                try {
                    new File(this.gemFlagsDirectory, gem).createNewFile();
                }
                catch (final IOException e) {
                    throw new MojoExecutionException("can not create empty file",
                            e);
                }
            }
        }
    }

    protected void ensureGem(final String gemName)
            throws MojoExecutionException {
        ensureGems(new String[] { gemName });
    }

    public void execute(final String args) throws MojoExecutionException {
        execute(args.trim().split("\\s+"), this.artifacts);
    }

    public void execute(final String[] args) throws MojoExecutionException {
        execute(args, this.artifacts);
    }

    private void execute(final String[] args, final Set<Artifact> artifacts)
            throws MojoExecutionException {
        try {
            getLog().info("jruby fork      : " + this.fork);
            getLog().info("launch directory: " + launchDirectory());
            final Launcher launcher = (this.fork ? new AntLauncher(getLog(),
                    this.jrubyHome,
                    this.gemHome,
                    this.gemPath,
                    this.jrubyLaunchMemory) : new EmbeddedLauncher(getLog(),
                    this.classRealm));
            launcher.execute(launchDirectory(),
                             args,
                             artifacts,
                             resolveJRUBYCompleteArtifact(),
                             this.outputDirectory);
        }
        catch (final DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("error creating launcher", e);
        }
    }
}
