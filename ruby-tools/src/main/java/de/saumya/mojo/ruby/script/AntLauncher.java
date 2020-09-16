/**
 *
 */
package de.saumya.mojo.ruby.script;

import de.saumya.mojo.ruby.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Environment.Variable;
import org.apache.tools.ant.types.Path;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

class AntLauncher extends AbstractLauncher {

    private static final String MAVEN_CLASSPATH = "maven.classpath";

    private static final String DEFAULT_XMX = "-Xmx384m";

    private static final String TEMP_FILE_PREFIX = "jruby-ant-launcher-";

    private final Logger logger;

    private final ScriptFactory factory;

    private final Project project;

    AntLauncher(final Logger logger, final ScriptFactory factory) {
        this.logger = logger;
        this.factory = factory;
        this.project = createAntProject();
    }

    @Override
    protected void doExecute(final File launchDirectory, 
                             final List<String> args, File outputFile) throws ScriptException, IOException {
        doExecute(launchDirectory, args,new FileOutputStream(outputFile));
    }

    @Override
    protected void doExecute(final File launchDirectory, final List<String> args,
                             final OutputStream outputStream) throws ScriptException, IOException {
        final Java java = new Java();
        java.setProject(this.project);
        java.setClassname("org.jruby.Main");
        java.setFailonerror(true);

        java.setFork(true);
        java.setDir(launchDirectory);

        for (final Map.Entry<String, String> entry : this.factory.environment().entrySet()) {
            Variable v = new Variable();
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

        Path temp = (Path) this.project.getReference(MAVEN_CLASSPATH);
        if (this.factory.jrubyJar != null) {
            temp.add(new Path(project, this.factory.jrubyJar.getAbsolutePath()));
        }
        java.createJvmarg().setLine("-XX:+IgnoreUnrecognizedVMOptions --add-opens=java.base/java.security.cert=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/java.util.zip=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/javax.crypto=ALL-UNNAMED --illegal-access=warn");
        java.createJvmarg().setValue("-cp");
        java.createJvmarg().setPath(temp);

        // Does not work on all JVMs
//        if (!factory.jvmArgs.matches("(-client|-server)")) {
//        	java.createJvmarg().setValue("-client");
//        }
        
        for (String arg : factory.jvmArgs.list) {
        	java.createJvmarg().setValue(arg);	
        }
        
        // hack to avoid jruby-core in bootclassloader where as the dependent jars are in system classloader
        if (this.factory.jrubyJar != null && this.factory.jrubyJar.equals(this.factory.jrubyStdlibJar)){
            java.createJvmarg().setValue("-Xbootclasspath/a:"
                + this.factory.jrubyJar.getAbsolutePath());
        }
        if ( this.factory.jrubyJar  == null && System.getProperty( "jruby.home" ) != null ){
            Variable v = new Variable();
            v.setKey( "jruby.home" );
            v.setValue( System.getProperty( "jruby.home" ) );
            java.addSysproperty( v );
            File lib =  System.getProperty("jruby.lib") != null ? new File( System.getProperty("jruby.lib") ) :
                new File( System.getProperty("jruby.home"), "lib" );
            File jrubyJar = new File( lib, "jruby.jar" );
            java.createJvmarg().setValue("-Xbootclasspath/a:"
                    + jrubyJar.getAbsolutePath());
        }

        File outputTempFile = null;
        if (outputStream != null) {
            outputTempFile = File.createTempFile(TEMP_FILE_PREFIX, ".output");
            java.setOutput(outputTempFile);
        }

        java.setLogError(true);
        File errorTempFile = null;
        try {
            errorTempFile = File.createTempFile(TEMP_FILE_PREFIX, ".log");
            errorTempFile.deleteOnExit();
            java.setError(errorTempFile);
            java.execute();

            if (outputStream != null) {
                writeInto(outputTempFile, outputStream);
                outputTempFile.delete();
            }
        } catch (IOException e) {
            logger.warn("can not create tempfile for stderr");
            java.execute();
        } finally {
            if (errorTempFile != null && errorTempFile.length() > 0) {
                try {
                    byte[] encoded = Files.readAllBytes(errorTempFile.toPath());
                    logger.warn(new String(encoded));
                } catch (IOException e) {
                    logger.warn("can not read error file");
                }
                errorTempFile.delete();
            }
        }
    }

    private void writeInto(File file, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[1024 * 4];
        final InputStream fileIS = new FileInputStream(file);

        while (fileIS.read(buffer) > 0) {
            outputStream.write(buffer);
        }
    }

    private Project createAntProject() {
        final Project project = new Project();

        // setup maven.plugin.classpath
        final Path classPath = new Path(project);
        for (final String path : this.factory.classpathElements) {
            if (!path.contains("jruby-complete") || factory.jrubyJar == null) {
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
        doExecute(null, args, (OutputStream) null);
    }

    @Override
    public void execute(final List<String> args, final File outputFile)
            throws ScriptException, IOException {
        doExecute(null, args, outputFile);
    }

    @Override
    public void executeIn(final File launchDirectory, final List<String> args)
            throws ScriptException, IOException {
        doExecute(launchDirectory, args, (OutputStream) null);
    }

    @Override
    public void executeIn(final File launchDirectory, final List<String> args,
            final File outputFile) throws ScriptException, IOException {
        doExecute(launchDirectory, args, outputFile);
    }

    @Override
    public void executeScript(final String script, final List<String> args)
            throws ScriptException, IOException {
        executeScript(script, args, (OutputStream) null);
    }

    @Override
    public void executeScript(final String script, final List<String> args,
            final File outputFile) throws ScriptException, IOException {
        executeScript(null, script, args, outputFile);
    }

    @Override
    public void executeScript(final File launchDirectory, final String script,
            final List<String> args) throws ScriptException, IOException {
        executeScript(launchDirectory, script, args, (OutputStream) null);
    }

    @Override
    public void executeScript(final File launchDirectory, final String script,
            final List<String> args, final File outputFile)
            throws ScriptException, IOException {
        addScriptArguments(script, args);
        doExecute(launchDirectory, args, outputFile);
    }

    @Override
    public void executeScript(final File launchDirectory, final String script,
                              final List<String> args, final OutputStream outputStream)
            throws ScriptException, IOException {
        addScriptArguments(script, args);
        doExecute(launchDirectory, args, outputStream);
    }

    private void addScriptArguments(String script, List<String> args) {
        args.add(0, "-e");
        args.add(1, script);
        args.add(2, "--");
    }

}
