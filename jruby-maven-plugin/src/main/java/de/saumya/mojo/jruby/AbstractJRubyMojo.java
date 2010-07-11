package de.saumya.mojo.jruby;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
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

    private static String              DEFAULT_JRUBY_VERSION = "1.5.1";

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
     * @parameter default-value="${launchDirectory}"
     */
    protected File                     launchDirectory;

    /**
     * directory of JRuby home to use when forking JRuby.
     * 
     * @parameter default-value="${jruby.home}"
     */
    protected File                     home;

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
    protected String                   launchMemory;

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
    protected String                   version;

    /**
     * output file where the stdout will be redirected to
     * 
     * @parameter
     */
    // TODO move into goals where needed
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
    protected ClassRealm               classRealm;

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

    protected Artifact resolveJRUBYCompleteArtifact()
            throws DependencyResolutionRequiredException,
            MojoExecutionException {
        if (this.version != null) {
            // preference to command line or property version
            return resolveJRUBYCompleteArtifact(this.version);
        }
        else {
            // then take jruby from the dependencies
            for (final Object o : this.project.getDependencies()) {
                final Dependency artifact = (Dependency) o;
                if (artifact.getArtifactId().equals("jruby-complete")
                        && !artifact.getScope().equals(Artifact.SCOPE_PROVIDED)
                        && !artifact.getScope().equals(Artifact.SCOPE_SYSTEM)) {
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
            this.launchDirectory = this.project.getBasedir();
            if (this.launchDirectory == null || !this.launchDirectory.exists()) {
                this.launchDirectory = new File(System.getProperty("user.dir"));
            }
        }
        return this.launchDirectory;
    }

    protected void execute(final String args, final boolean resolveArtifacts)
            throws MojoExecutionException {
        execute(args, resolveArtifacts, new HashMap<String, String>());
    }

    protected void executeScript(final File script, final String args,
            final boolean resolveArtifacts, final Map<String, String> env)
            throws MojoExecutionException {
        final StringBuilder buf = new StringBuilder("-e ");
        setupEnv(env);
        for (final Map.Entry<String, String> entry : env.entrySet()) {
            buf.append("ENV['")
                    .append(entry.getKey())
                    .append("']='")
                    .append(entry.getValue())
                    .append("';");
        }
        buf.append("';ARGV<<[")
                .append(args)
                .append("];ARGV.flatten!;load('")
                .append(script)
                .append("');");
        execute(buf.toString(), resolveArtifacts, new HashMap<String, String>());
    }

    protected void execute(final String args, final boolean resolveArtifacts,
            final Map<String, String> env) throws MojoExecutionException {
        execute(args.trim().split("\\s+"),
                this.artifacts,
                this.outputFile,
                resolveArtifacts,
                env);
    }

    protected void execute(final String args) throws MojoExecutionException {
        execute(args, true);
    }

    protected void execute(final String[] args) throws MojoExecutionException {
        execute(args,
                this.artifacts,
                this.outputFile,
                true,
                new HashMap<String, String>());
    }

    protected void execute(final String[] args, final File outputFile)
            throws MojoExecutionException {
        execute(args,
                this.artifacts,
                outputFile,
                true,
                new HashMap<String, String>());
    }

    protected void execute(final String[] args, final File outputFile,
            final boolean resolveArtifacts) throws MojoExecutionException {
        execute(args,
                this.artifacts,
                outputFile,
                resolveArtifacts,
                new HashMap<String, String>());
    }

    private void execute(final String[] args, final Set<Artifact> artifacts,
            final File outputFile, final boolean resolveArtifacts,
            final Map<String, String> env) throws MojoExecutionException {
        final Set<Artifact> artis = new HashSet<Artifact>(artifacts);
        if (resolveArtifacts) {
            if (this.project.getArtifact().getFile() != null
                    && this.project.getArtifact().getFile().exists()) {
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
                setupEnv(env);
                launcher = new AntLauncher(getLog(),
                        this.home,
                        env,
                        this.launchMemory,
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

    private void setupEnv(final Map<String, String> env) {
        if (this.gemHome != null) {
            env.put(AntLauncher.GEM_HOME, this.gemHome.getAbsolutePath());
        }
        if (this.gemPath != null) {
            env.put(AntLauncher.GEM_PATH, this.gemPath.getAbsolutePath());
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
                                                                                   this.project.getManagedVersionMap(),
                                                                                   this.localRepository,
                                                                                   this.remoteRepositories,
                                                                                   this.metadata,
                                                                                   new ArtifactFilter() {
                                                                                       public boolean include(
                                                                                               final Artifact artifact) {
                                                                                           return artifact.getType()
                                                                                                   .equals("gem");
                                                                                       }
                                                                                   });
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
