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
import org.apache.tools.ant.types.Path;
import org.codehaus.plexus.util.StringUtils;

abstract class AbstractAntLauncher {

    void execute(final Log log, final File launchDirectory,
            final File jrubyHome, final File jrubyGemHome,
            final File jrubyGemPath, final List<String> args,
            final Set<Artifact> artifacts, final Artifact jrubyArtifact)
            throws MojoExecutionException,
            DependencyResolutionRequiredException {
        final Project project = getProject(launchDirectory,
                                           log,
                                           artifacts,
                                           jrubyArtifact);
        execute(log,
                launchDirectory,
                jrubyHome,
                jrubyGemHome,
                jrubyGemPath,
                args,
                project);
    }

    abstract protected void execute(Log log, File launchDirector,
            File jrubyHome, File jrubyGemHome, File jrubyGemPath,
            List<String> args, Project project) throws MojoExecutionException;

    protected Project getProject(final File launchDirectory, final Log log,
            final Set<Artifact> artifacts, final Artifact jrubyArtifact)
            throws MojoExecutionException,
            DependencyResolutionRequiredException {
        final Project project = new Project();
        project.setBaseDir(launchDirectory);
        project.addBuildListener(new LogAdapter(log));
        addReference(project, "maven.compile.classpath", artifacts);

        // setup maven.plugin.classpath
        log.info("jruby version   : " + jrubyArtifact.getVersion());

        final Path jrubyJarPath = new Path(project, jrubyArtifact.getFile()
                .getAbsolutePath());
        project.addReference("maven.plugin.classpath", jrubyJarPath);
        return project;
    }

    protected void addReference(final Project project, final String reference,
            final Collection<Artifact> artifacts)
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
