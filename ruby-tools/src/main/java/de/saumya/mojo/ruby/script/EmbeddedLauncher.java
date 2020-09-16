/**
 *
 */
package de.saumya.mojo.ruby.script;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.DuplicateRealmException;
import org.codehaus.classworlds.NoSuchRealmException;

import de.saumya.mojo.ruby.Logger;

class EmbeddedLauncher extends AbstractLauncher {

    private static final String TEMP_FILE_PREFIX = "jruby-embedded-launcher-";

    private static final Class<?>[] No_ARG_TYPES = new Class[0];
    private static final Object[]   NO_ARGS      = new Object[0];

    private final ClassRealm        classRealm;
    private final ScriptFactory     factory;
    private final Logger            logger;

    public EmbeddedLauncher(final Logger logger, final ScriptFactory factory)
            throws ScriptException {
        this.logger = logger;
        this.factory = factory;
        if (factory.classRealm != null) {
            this.classRealm = cloneClassRealm(factory.jrubyJar,
                                              factory.classpathElements,
                                              factory.classRealm);
        }
        else {
            this.classRealm = null;
        }
    }

    private ClassRealm cloneClassRealm(final File jrubyJar,
            final List<String> classpathElements, final ClassRealm classRealm)
            throws ScriptException {
        // TODO how to reuse the plugin realm ?
//        for (final String classpath : classpathElements) {
//            if (classpath.equals(jrubyJar.getAbsolutePath())) {
//                return null;
//            }
//        }
        ClassRealm newClassRealm;
        try {
            ClassRealm jruby;
            try {
                jruby = classRealm.getWorld().getRealm("jruby");
            }
            catch (final NoSuchRealmException e) {
                jruby = classRealm.getWorld().newRealm("jruby");
                if(jrubyJar != null){
                    jruby.addConstituent(jrubyJar.toURI().toURL());
                }
            }
            try {
                jruby.getWorld().disposeRealm("pom");
            }
            catch (final NoSuchRealmException e) {
                // ignored
            }
            newClassRealm = jruby.createChildRealm("pom");
            for (final String classpath : classpathElements) {
                if (!classpath.contains("jruby-complete") || factory.jrubyJar == null) {
                    newClassRealm.addConstituent(new File(classpath).toURI()
                            .toURL());
                }
            }
        }
        catch (final DuplicateRealmException e) {
            throw new ScriptException("error in naming realms", e);
        }
        catch (final MalformedURLException e) {
            throw new ScriptException("hmm. found some malformed URL", e);
        }

        return newClassRealm;
    }

    @Override
    protected void doExecute(final File launchDirectory,
            final List<String> args, final File outputFile)
            throws ScriptException, IOException {
        doExecute(launchDirectory, outputFile, args, true);
    }

    @Override
    protected void doExecute(final File launchDirectory,
            final List<String> args, OutputStream outputStream)
            throws ScriptException, IOException {
        final File outputFile = Files.createTempFile(TEMP_FILE_PREFIX, ".output").toFile();
        doExecute(launchDirectory, outputFile, args, true);
        byte[] outputBytes = FileUtils.readFileToByteArray(outputFile);
        outputStream.write(outputBytes);
    }

    private void doExecute(final File launchDirectory, final File outputFile,
            final List<String> args, final boolean warn)
            throws ScriptException, IOException {
        final String currentDir;
        if (launchDirectory != null) {
            currentDir = System.getProperty("user.dir");
            logger.debug("launch directory: "
                    + launchDirectory.getAbsolutePath());
            System.setProperty("user.dir", launchDirectory.getAbsolutePath());
        }
        else {
            currentDir = null;
        }

        args.addAll(0, this.factory.switches.list);

        if (warn) {
            if (this.factory.jvmArgs.list.size() > 0) {
                this.logger.warn("have to ignore jvm arguments and properties in the current setup");
            }
            if (this.factory.environment().size() > 0) {
                this.logger.warn("have to ignore environment settings in the current setup");
            }
        }

        final PrintStream output = System.out;
        ClassLoader current = null;
        try {
            if (outputFile != null) {
                final PrintStream writer = new PrintStream(outputFile);
                System.setOut(writer);
                this.logger.debug("output file: " + outputFile);
            }
            this.logger.debug("args: " + args);

            if (this.classRealm != null) {
                current = Thread.currentThread().getContextClassLoader();
                Thread.currentThread()
                        .setContextClassLoader(this.classRealm.getClassLoader());
            }
            // use reflection to avoid having jruby as plugin dependency
            final Class<?> clazz = Thread.currentThread()
                    .getContextClassLoader()
                    .loadClass("org.jruby.Main");
            final Object main = clazz.newInstance();
            final Method m = clazz.getMethod("run", String[].class);
            final long start = System.currentTimeMillis();
            final Object result = m.invoke(main,
                                           (Object) args.toArray(new String[args.size()]));
            final long end = System.currentTimeMillis();

            this.logger.debug("time " + (end - start));

            final int status;
            if (result instanceof Integer) {
                // jruby before version 1.5
                status = ((Integer) result);
            }
            else {
                // jruby from version 1.5 onwards
                // TODO better error handling like error messages and . . .
                // TODO see org.jruby.Main
                final Method statusMethod = result.getClass()
                        .getMethod("getStatus", No_ARG_TYPES);
                status = (Integer) statusMethod.invoke(result, NO_ARGS);
            }
            if (status != 0) {
                throw new ScriptException("some error in script " + args
                        + ": " + status);
            }

        }
        catch (final InstantiationException e) {
            throw new ScriptException(e);
        }
        catch (final IllegalAccessException e) {
            throw new ScriptException(e);
        }
        catch (final ClassNotFoundException e) {
            throw new ScriptException(e);
        }
        catch (final NoSuchMethodException e) {
            throw new ScriptException(e);
        }
        catch (final InvocationTargetException e) {
            throw new ScriptException(e);
        }
        finally {
            if (current != null) {
                Thread.currentThread().setContextClassLoader(current);
            }
            System.setOut(output);
            if (currentDir != null) {
                System.setProperty("user.dir", currentDir);
            }
            if (this.classRealm != null) {
                try {
                    this.classRealm.getWorld().disposeRealm("pom");
                    // jrubyClassRealm.setParent(null);
                }
                catch (final NoSuchRealmException e) {
                    // ignore
                }
            }

        }
    }

    @Override
    public void executeScript(final File launchDirectory,
        final String script, final List<String> args,
        final OutputStream outputStream) throws ScriptException, IOException {
        final File outputFile = Files.createTempFile(TEMP_FILE_PREFIX, ".output").toFile();
        executeScript(launchDirectory, script, args, outputFile);
        byte[] outputBytes = FileUtils.readFileToByteArray(outputFile);
        outputStream.write(outputBytes);
    }

    public void executeScript(final File launchDirectory, final String script,
            final List<String> args, final File outputFile)
            throws ScriptException, IOException {
        final StringBuilder buf = new StringBuilder();
        for (final Map.Entry<String, String> entry : this.factory.environment().entrySet()) {
            if (entry.getValue() != null) {
                buf.append("ENV['")
                        .append(entry.getKey())
                        .append("']='")
                        .append(entry.getValue())
                        .append("';");
            }
        }
        buf.append(script);

        args.add(0, "-e");
        args.add(1, buf.toString());
        args.add(2, "--");
        doExecute(launchDirectory, outputFile, args, false);
    }
}
