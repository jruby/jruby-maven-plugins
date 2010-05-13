package de.saumya.mojo.jruby;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.classworlds.ClassRealm;

/**
 * Base for all JRuby mojos.
 *
 * @requiresProject false
 */
public abstract class AbstractJRubyMojo extends AbstractMojo {

    private static String              DEFAULT_JRUBY_VERSION = "1.5.0";

    private static final Set<Artifact> NO_ARTIFACTS          = Collections.emptySet();

    /**
     * fork the JRuby execution.
     *
     * @parameter expression="${jruby.fork}" default-value="true"
     */
    protected boolean                  fork;

    /**
     * verbose jruby related output
     *
     * @parameter expression="${jruby.verbose}" default-value="false"
     */
    protected boolean                  verbose;

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
     * @parameter expression="${jruby.gem.home}"
     *            default-value="${project.build.directory}/rubygems"
     */
    protected File                     gemHome;

    /**
     * directory of JRuby path to use when forking JRuby.
     *
     * @parameter expression="${jruby.gem.path}"
     *            default-value="${project.build.directory}/rubygems"
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
    protected MavenProject             project;

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
    protected ArtifactFactory          artifactFactory;

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
    protected List<ArtifactRepository> remoteRepositories;

    /**
     * if the pom.xml has no runtime dependency to a jruby-complete.jar then
     * this version is used to resolve the jruby-complete dependency from the
     * local/remote maven repository. defaults to "1.4.1".
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
    @Deprecated
    private File                       gemFlagsDirectory;

    /**
     * output file where the stdout will be redirected to
     *
     * @parameter
     */
    protected File                     outputFile;

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

    /**
     * @component
     */
    protected ArtifactMetadataSource   metadata;

    /**
     * @component
     */
    protected MavenProjectBuilder      builder;

    private Artifact resolveJRUBYCompleteArtifact(final String version)
            throws DependencyResolutionRequiredException {
        getLog().debug("resolve jruby for verions " + version);
        final Artifact artifact = this.artifactFactory.createArtifactWithClassifier("org.jruby",
                                                                                    "jruby-complete",
                                                                                    version,
                                                                                    "jar",
                                                                                    null);
        return resolveJRUBYCompleteArtifact(artifact);
    }

    private Artifact resolveJRUBYCompleteArtifact(final Artifact artifact)
            throws DependencyResolutionRequiredException {
        try {
            // final ArtifactResolutionRequest request = new
            // ArtifactResolutionRequest();
            // request.setArtifact(artifact);
            // request.setLocalRepository(this.localRepository);
            // request.setRemoteRepostories(this.remoteRepositories);
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

        if (this.verbose) {
            getLog().info("jruby version   : " + artifact.getVersion());
        }
        return artifact;
    }

    private Artifact resolveJRUBYCompleteArtifact()
            throws DependencyResolutionRequiredException,
            MojoExecutionException {
        if (this.jrubyVersion != null) {
            // preference to command line or property version
            return resolveJRUBYCompleteArtifact(this.jrubyVersion);
        }
        else {
            // then take jruby from the dependencies
            for (final Object o : this.project.getDependencies()) {
                final Dependency artifact = (Dependency) o;
                if (artifact.getArtifactId().equals("jruby-complete")
                        && !artifact.getScope().equals(Artifact.SCOPE_PROVIDED)
                        && !artifact.getScope().equals(Artifact.SCOPE_SYSTEM)) {
                    // if (this.jrubyVersion != null
                    // && !this.jrubyVersion.equals(artifact.getVersion())) {
                    // getLog().warn("the configured jruby-version gets ignored in preference to the jruby dependency");
                    // }
                    return resolveJRUBYCompleteArtifact(this.artifactFactory.createArtifact(artifact.getGroupId(),
                                                                                            artifact.getArtifactId(),
                                                                                            artifact.getVersion(),
                                                                                            artifact.getScope(),
                                                                                            artifact.getType()));
                }
            }
        }
        // take the default version of jruby
        return resolveJRUBYCompleteArtifact(DEFAULT_JRUBY_VERSION);
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

    @Deprecated
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
                    NO_ARTIFACTS,
                    null,
                    true);

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

    @Deprecated
    protected void ensureGem(final String gemName)
            throws MojoExecutionException {
        ensureGems(new String[] { gemName });
    }

    protected void execute(final String args, final boolean resolveArtifacts)
            throws MojoExecutionException {
        execute(args.trim().split("\\s+"),
                this.artifacts,
                this.outputFile,
                resolveArtifacts);
    }

    protected void execute(final String args) throws MojoExecutionException {
        execute(args, true);
    }

    protected void execute(final String[] args) throws MojoExecutionException {
        execute(args, this.artifacts, this.outputFile, true);
    }

    protected void execute(final String[] args, final File outputFile)
            throws MojoExecutionException {
        execute(args, this.artifacts, outputFile, true);
    }

    protected void execute(final String[] args, final File outputFile,
            final boolean resolveArtifacts) throws MojoExecutionException {
        execute(args, this.artifacts, outputFile, false);
    }

    private void execute(final String[] args, final Set<Artifact> artifacts,
            final File outputFile, final boolean resolveArtifacts)
            throws MojoExecutionException {
        final Set<Artifact> artis = new HashSet<Artifact>(artifacts);
        if (resolveArtifacts) {
            if (this.project.getArtifact().getFile() != null) {
                resolveTransitively(artis, this.project.getArtifact());
            }

            final Iterator<Artifact> iterator = artis.iterator();
            while (iterator.hasNext()) {
                final Artifact artifact = iterator.next();
                if (artifact.getArtifactHandler().getPackaging().equals("gem")) {
                    // TODO maybe better remove them at the end
                    iterator.remove();
                }
            }
        }
        try {
            if (this.verbose) {
                getLog().info("jruby fork      : " + this.fork);
                getLog().info("launch directory: " + launchDirectory());
                getLog().info("jruby args      : " + Arrays.toString(args));
            }
            final Launcher launcher;
            if (this.fork) {
                launcher = new AntLauncher(getLog(),
                        this.jrubyHome,
                        this.gemHome,
                        this.gemPath,
                        this.jrubyLaunchMemory,
                        this.verbose);
            }
            else {
                launcher = new EmbeddedLauncher(getLog(), this.classRealm);
            }
            launcher.execute(launchDirectory(),
                             args,
                             artis,
                             resolveJRUBYCompleteArtifact(),
                             this.outputDirectory,
                             outputFile);
        }
        catch (final DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("error creating launcher", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected void resolveTransitively(final Collection<Artifact> artifacts,
            final Artifact artifact) throws MojoExecutionException {
        // System.out.println(artifact + " resolve:");
        // if (artifact.getArtifactHandler().isIncludesDependencies()) {
        try {
            final MavenProject mavenProject = this.builder.buildFromRepository(artifact,
                                                                               this.remoteRepositories,
                                                                               this.localRepository);
            final Set<Artifact> moreArtifacts = mavenProject.createArtifacts(this.artifactFactory,
                                                                             null,
                                                                             null);
            final ArtifactResolutionResult arr = this.resolver.resolveTransitively(moreArtifacts,
                                                                                   artifact,
                                                                                   this.remoteRepositories,
                                                                                   this.localRepository,
                                                                                   this.metadata);
            // System.out.println(artifact + " " + arr);
            for (final Object artiObject : arr.getArtifacts()) {
                // allow older api to work
                final Artifact arti = (Artifact) artiObject;
                artifacts.add(arti);
            }
        }
        catch (final ArtifactResolutionException e) {
            throw new MojoExecutionException("error resolving " + artifact, e);
        }
        catch (final ArtifactNotFoundException e) {
            throw new MojoExecutionException("error resolving " + artifact, e);
        }
        catch (final InvalidDependencyVersionException e) {
            throw new MojoExecutionException("error resolving " + artifact, e);
        }
        catch (final ProjectBuildingException e) {
            throw new MojoExecutionException("error building project for "
                    + artifact, e);
        }
    }

    protected String fileFromClassloader(final String file) {
        return Thread.currentThread()
                .getContextClassLoader()
                .getResource(file)
                .toExternalForm();
    }
}
