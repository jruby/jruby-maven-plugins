/**
 *
 */
package de.saumya.mojo.jruby;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Commandline.Argument;
import org.apache.tools.ant.types.Environment.Variable;
import org.codehaus.plexus.util.StringUtils;

class AntLauncher extends AbstractLauncher {

    private final File   jrubyHome;

    private final File   jrubyGemHome;

    private final File   jrubyGemPath;

    private final String jrubyLaunchMemory;

    AntLauncher(final Log log, final File jrubyHome, final File jrubyGemHome,
            final File jrubyGemPath, final String jrubyLaunchMemory) {
        super(log);
        this.jrubyGemHome = jrubyGemHome;
        this.jrubyGemPath = jrubyGemPath;
        this.jrubyHome = jrubyHome;
        this.jrubyLaunchMemory = jrubyLaunchMemory;
    }

    public void execute(final File launchDirectory, final String[] args,
            final Set<Artifact> artifacts, final Artifact jrubyArtifact,
            final File classesDirectory) throws MojoExecutionException,
            DependencyResolutionRequiredException {
        final Project project = getProject(launchDirectory,
                                           artifacts,
                                           jrubyArtifact,
                                           classesDirectory);
        execute(launchDirectory, args, project);
    }

    protected void execute(final File launchDirectory, final String[] args,
            final Project project) throws MojoExecutionException {
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
        if (this.jrubyGemHome != null) {
            final Variable v = new Variable();
            v.setKey("GEM_HOME");
            v.setValue(this.jrubyGemHome.getAbsolutePath());
            java.addEnv(v);
        }
        if (this.jrubyGemPath != null) {
            final Variable v = new Variable();
            v.setKey("GEM_PATH");
            v.setValue(this.jrubyGemPath.getAbsolutePath());
            java.addEnv(v);
        }
        getLog().info("java classpath  : " + p.toString());

        for (final String arg : args) {
            java.createArg().setValue(arg);
        }
        java.createJvmarg().setValue("-cp");
        java.createJvmarg().setPath(p);

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

    public static class LogAdapter implements BuildListener {
        private final Log log;

        LogAdapter(final Log log) {
            this.log = log;
        }

        public void buildStarted(final BuildEvent event) {
            log(event);
        }

        public void buildFinished(final BuildEvent event) {
            log(event);
        }

        public void targetStarted(final BuildEvent event) {
            log(event);
        }

        public void targetFinished(final BuildEvent event) {
            log(event);
        }

        public void taskStarted(final BuildEvent event) {
            log(event);
        }

        public void taskFinished(final BuildEvent event) {
            log(event);
        }

        public void messageLogged(final BuildEvent event) {
            log(event);
        }

        private void log(final BuildEvent event) {
            final int priority = event.getPriority();
            switch (priority) {
            case Project.MSG_ERR:
                this.log.error(event.getMessage());
                break;

            case Project.MSG_WARN:
                this.log.warn(event.getMessage());
                break;

            case Project.MSG_INFO:
                this.log.info(event.getMessage());
                break;

            case Project.MSG_VERBOSE:
                this.log.debug(event.getMessage());
                break;

            case Project.MSG_DEBUG:
                this.log.debug(event.getMessage());
                break;

            default:
                this.log.info(event.getMessage());
                break;
            }
        }
    }
}
