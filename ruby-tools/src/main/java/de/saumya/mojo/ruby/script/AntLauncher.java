/**
 *
 */
package de.saumya.mojo.ruby.script;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Environment.Variable;

import de.saumya.mojo.ruby.Logger;

class AntLauncher extends AbstractLauncher {

    private static final String MAVEN_CLASSPATH = "maven.classpath";

    private final Logger        logger;

    private final ScriptFactory factory;

    private final Project       project;

    AntLauncher(final Logger logger, final ScriptFactory factory) {
        this.logger = logger;
        this.factory = factory;
        this.project = createAntProject();
    }

    @Override
    protected void doExecute(final File launchDirectory,
            final List<String> args, final File outputFile) {
        final Java java = new Java();
        java.setProject(this.project);
        java.setClassname("org.jruby.Main");
        java.setFailonerror(true);

        java.setFork(true);
        java.setDir(launchDirectory);

        // if (this.jrubyHome != null) {
        // final Variable v = new Variable();
        // v.setKey("jruby.home");
        // v.setValue(this.jrubyHome.getAbsolutePath());
        // java.addSysproperty(v);
        // }
        for (final Map.Entry<String, String> entry : this.factory.env.entrySet()) {
            final Variable v = new Variable();
            v.setKey(entry.getKey());
            v.setValue(entry.getValue());
            java.addEnv(v);
        }
        this.logger.debug("java classpath  : "
                + this.project.getReference(MAVEN_CLASSPATH));
        if (this.factory.env.size() > 0) {
            this.logger.info("environment     :");
            for (final Map.Entry<String, String> entry : this.factory.env.entrySet()) {
                this.logger.info("\t\t" + entry.getKey() + " => "
                        + entry.getValue());
            }
        }

        for (final String arg : args) {
            java.createArg().setValue(arg);
        }
        java.createJvmarg().setValue("-cp");
        java.createJvmarg()
                .setPath((Path) this.project.getReference(MAVEN_CLASSPATH));
        java.createJvmarg().setValue("-client");
        java.createJvmarg().setValue("-Xbootclasspath/a:"
                + this.factory.jrubyJar.getAbsolutePath());

        if (outputFile != null) {
            java.setOutput(outputFile);
        }
        java.execute();
    }

    private Project createAntProject() {
        final Project project = new Project();

        // setup maven.plugin.classpath
        final Path classPath = new Path(project);
        for (final String path : this.factory.classpathElements) {
            if (!path.contains("jruby-complete")) {
                classPath.add(new Path(project, path));
            }
        }

        project.addReference(MAVEN_CLASSPATH, classPath);
        project.addBuildListener(new AntLogAdapter(this.logger));
        return project;
    }

    @Override
    public void execute(final List<String> args) throws ScriptException,
            IOException {
        doExecute(null, args, null);
    }

    @Override
    public void execute(final List<String> args, final File outputFile)
            throws ScriptException, IOException {
        doExecute(null, args, outputFile);
    }

    @Override
    public void executeIn(final File launchDirectory, final List<String> args)
            throws ScriptException, IOException {
        doExecute(launchDirectory, args, null);
    }

    @Override
    public void executeIn(final File launchDirectory, final List<String> args,
            final File outputFile) throws ScriptException, IOException {
        doExecute(launchDirectory, args, outputFile);
    }

    @Override
    public void executeScript(final String script, final List<String> args)
            throws ScriptException, IOException {
        executeScript(script, args, null);
    }

    @Override
    public void executeScript(final String script, final List<String> args,
            final File outputFile) throws ScriptException, IOException {
        executeScript(null, script, args, outputFile);
    }

    @Override
    public void executeScript(final File launchDirectory, final String script,
            final List<String> args) throws ScriptException, IOException {
        executeScript(launchDirectory, script, args, null);
    }

    public void executeScript(final File launchDirectory, final String script,
            final List<String> args, final File outputFile)
            throws ScriptException, IOException {
        args.add(0, "-e");
        args.add(1, script);
        args.add(2, "--");
        doExecute(launchDirectory, args, outputFile);
    }
}
