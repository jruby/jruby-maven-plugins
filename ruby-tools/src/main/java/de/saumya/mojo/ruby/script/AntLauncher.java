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

    private static final String DEFAULT_XMX = "-Xmx384m";

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

        for (final Map.Entry<String, String> entry : this.factory.environment().entrySet()) {
            final Variable v = new Variable();
            v.setKey(entry.getKey());
            v.setValue(entry.getValue());
            java.addEnv(v);
        }
        // TODO add isDebugable to the logger and log only when debug is needed
        this.logger.debug("java classpath  : "
                + this.project.getReference(MAVEN_CLASSPATH));
        if (this.factory.environment().size() > 0) {
            this.logger.debug("environment     :");
            for (final Map.Entry<String, String> entry : this.factory.environment().entrySet()) {
                this.logger.debug("\t\t" + entry.getKey() + " => "
                        + entry.getValue());
            }
        }

        for (final String arg : factory.switches.list) {
            java.createArg().setValue(arg);
        }
        for (final String arg : args) {
            java.createArg().setValue(arg);
        }

        java.createJvmarg().setValue("-cp");
        java.createJvmarg()
                .setPath((Path) this.project.getReference(MAVEN_CLASSPATH));

        if (!factory.jvmArgs.matches("(-client|-server)")) {
        	java.createJvmarg().setValue("-client");	
        } 
        
        if (!factory.jvmArgs.matches("-Xmx\\d+m")) {
        	java.createJvmarg().setValue(DEFAULT_XMX);	
        } 
        
        for (String arg : factory.jvmArgs.list) {
        	java.createJvmarg().setValue(arg);	
        }
        
        // hack to avoid jruby-core in bootclassloader where as the dependent jars are in system classloader
        if (this.factory.jrubyJar != null && this.factory.jrubyJar.equals(this.factory.jrubyStdlibJar)){
        java.createJvmarg().setValue("-Xbootclasspath/a:"
                + this.factory.jrubyJar.getAbsolutePath());
        }
        
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
