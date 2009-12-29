package de.saumya.mojo.jruby;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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

    /**
     * The project compile classpath.
     * 
     * @parameter default-value="${project.compileClasspathElements}"
     * @required
     * @readonly
     */
    private List                       compileClasspathElements;
    /**
     * The project compile classpath.
     * 
     * @parameter default-value="${project.testClasspathElements}"
     * @required
     * @readonly
     */
    private List                       testClasspathElements;

    // /**
    // * @parameter expression="${plugin}"
    // */
    // private PluginDescriptor plugin;

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
        final Artifact artifact = this.artifactFactory.createArtifactWithClassifier("org.jruby",
                                                                                    "jruby-complete",
                                                                                    version,
                                                                                    "jar",
                                                                                    null);
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

        getLog().info("jruby version   : " + version);
        return artifact;
    }

    private Artifact resolveJRUBYCompleteArtifact()
            throws DependencyResolutionRequiredException,
            MojoExecutionException {
        for (final Object o : this.project.getDependencyArtifacts()) {
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
        // System.out.println("\n\n\n");
        // System.out.println(this.compileClasspathElements);
        // System.out.println("\n\n\n");
        // System.out.println(this.testClasspathElements);
        // System.out.println("\n\n\n");
        final Set<Artifact> artis = new HashSet<Artifact>();
        resolveTransitively(artis, this.project.getArtifact());

        final Iterator<Artifact> iterator = artis.iterator();
        while (iterator.hasNext()) {
            final Artifact artifact = iterator.next();
            // System.out.println(artifact + " -> "
            // + artifact.getArtifactHandler().isAddedToClasspath() + " "
            // + artifact.getArtifactHandler().isIncludesDependencies());
            if (artifact.getArtifactHandler().getPackaging().equals("gem")) {
                // TODO maybe better remove them add the end
                iterator.remove();
            }
            // resolveTransitively(artifacts, artifact);
        }
        // System.out.println(artis);
        try {
            getLog().info("jruby fork      : " + this.fork);
            getLog().info("launch directory: " + launchDirectory());
            getLog().info("jruby args      : " + Arrays.toString(args));
            final Launcher launcher = (this.fork ? new AntLauncher(getLog(),
                    this.jrubyHome,
                    this.gemHome,
                    this.gemPath,
                    this.jrubyLaunchMemory) : new EmbeddedLauncher(getLog(),
                    this.classRealm));
            launcher.execute(launchDirectory(),
                             args,
                             artis,
                             resolveJRUBYCompleteArtifact(),
                             this.outputDirectory);
        }
        catch (final DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("error creating launcher", e);
        }
    }

    private void resolveTransitively(final Set<Artifact> artifacts,
            final Artifact artifact) {
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
                // System.out.println(arti
                // + " "
                // + "java".equals(arti.getArtifactHandler()
                // .isIncludesDependencies()) + " "
                // + arti.getArtifactHandler().getExtension() + " "
                // + arti.getArtifactHandler().getPackaging() + " "
                // + arti.getArtifactHandler().getClassifier());
                if (!artifacts.contains(arti)
                // TODO do not handle gem only artifacts for now
                        && !(!arti.hasClassifier() && "gem".equals(arti.getArtifactHandler()
                                .getPackaging()))) {
                    resolveTransitively(artifacts, arti);
                }
                artifacts.add(arti);
            }
        }
        catch (final ArtifactResolutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (final ArtifactNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (final InvalidDependencyVersionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (final ProjectBuildingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // }
    }
}
