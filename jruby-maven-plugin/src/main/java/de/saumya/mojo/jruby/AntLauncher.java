/**
 *
 */
package de.saumya.mojo.jruby;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Commandline.Argument;
import org.apache.tools.ant.types.Environment.Variable;
import org.codehaus.plexus.util.StringUtils;

class AntLauncher extends AbstractLauncher {

    static final String               GEM_HOME = "GEM_HOME";

    static final String               GEM_PATH = "GEM_PATH";

    private final File                jrubyHome;

    private final Map<String, String> env;

    private final String              jrubyLaunchMemory;

    private final boolean             verbose;

    AntLauncher(final Log log, final File jrubyHome,
            final Map<String, String> env, final String jrubyLaunchMemory,
            final boolean verbose) {
        super(log);
        this.env = env;
        this.jrubyHome = jrubyHome;
        this.jrubyLaunchMemory = jrubyLaunchMemory;
        this.verbose = verbose;
    }

    public void execute(final File launchDirectory, final String[] args,
            final Set<Artifact> artifacts, final Artifact jrubyArtifact,
            final File classesDirectory, final File outputFile)
            throws MojoExecutionException,
            DependencyResolutionRequiredException {
        final Project project = getProject(launchDirectory,
                                           artifacts,
                                           jrubyArtifact,
                                           classesDirectory);
        execute(launchDirectory, args, project, outputFile);
    }

    protected void execute(final File launchDirectory, final String[] args,
            final Project project, final File outputFile)
            throws MojoExecutionException {
        final Java java = new Java();
        java.setProject(project);
        java.setClassname("org.jruby.Main");
        java.setFailonerror(true);

        java.setFork(true);
        java.setDir(launchDirectory);

        final Argument memoryArg = java.createJvmarg();
        memoryArg.setValue("-Xmx" + this.jrubyLaunchMemory);

        final Variable classpath = new Variable();

        final Path p = new Path(java.getProject());
        p.add((Path) project.getReference("maven.plugin.classpath"));
        p.add((Path) project.getReference("maven.compile.classpath"));
        classpath.setKey("JRUBY_PARENT_CLASSPATH");
        classpath.setValue(p.toString());

        java.addEnv(classpath);

        if (this.jrubyHome != null) {
            final Variable v = new Variable();
            v.setKey("jruby.home");
            v.setValue(this.jrubyHome.getAbsolutePath());
            java.addSysproperty(v);
        }
        for (final Map.Entry<String, String> entry : this.env.entrySet()) {
            final Variable v = new Variable();
            v.setKey(entry.getKey());
            v.setValue(entry.getValue());
            java.addEnv(v);
        }
        if (this.verbose) {
            getLog().info("java classpath  : " + p.toString());
            if (this.env.size() > 0) {
                getLog().info("environment     :");
                for (final Map.Entry<String, String> entry : this.env.entrySet()) {
                    getLog().info("\t\t" + entry.getKey() + " => "
                            + entry.getValue());
                }
            }
        }

        for (final String arg : args) {
            java.createArg().setValue(arg);
        }
        java.createJvmarg().setValue("-cp");
        java.createJvmarg().setPath(p);

        if (outputFile != null) {
            java.setOutput(outputFile);
            java.setError(new File(outputFile.getParentFile(),
                    outputFile.getName() + ".errors"));
        }
        java.execute();
    }

    protected Project getProject(final File launchDirectory,
            final Set<Artifact> artifacts, final Artifact jrubyArtifact,
            final File classesDirectory) throws MojoExecutionException,
            DependencyResolutionRequiredException {
        final Project project = new Project();
        project.setBaseDir(launchDirectory);
        project.addBuildListener(new LogAdapter(getLog()));
        addReference(project,
                     "maven.compile.classpath",
                     artifacts,
                     classesDirectory);

        // setup maven.plugin.classpath
        final Path jrubyJarPath = new Path(project, jrubyArtifact.getFile()
                .getAbsolutePath());
        project.addReference("maven.plugin.classpath", jrubyJarPath);
        return project;
    }

    protected void addReference(final Project project, final String reference,
            final Collection<Artifact> artifacts, final File classesDirectory)
            throws MojoExecutionException,
            DependencyResolutionRequiredException {
        final List<File> list = new ArrayList<File>(artifacts.size());

        for (final Artifact a : artifacts) {
            final File path = a.getFile();
            if (path == null) {
                throw new DependencyResolutionRequiredException(a);
            }
            if (path.exists() && !a.getArtifactId().equals("jruby-complete")) {
                list.add(path);
            }
        }

        if (classesDirectory != null && classesDirectory.exists()) {
            list.add(classesDirectory);
        }
        final Path path = new Path(project);
        path.setPath(StringUtils.join(list.iterator(), File.pathSeparator));
        project.addReference(reference, path);
    }
}
