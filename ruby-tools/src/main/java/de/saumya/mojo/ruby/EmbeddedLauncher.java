/**
 *
 */
package de.saumya.mojo.ruby;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.DuplicateRealmException;
import org.codehaus.classworlds.NoSuchRealmException;

class EmbeddedLauncher extends AbstractLauncher {

    private static final Class<?>[] No_ARG_TYPES = new Class[0];
    private static final Object[]   NO_ARGS      = new Object[0];

    private final ClassRealm        classRealm;
    private final ScriptFactory     factory;
    private final Logger            logger;

    public EmbeddedLauncher(final Logger logger, final ScriptFactory factory)
            throws RubyScriptException {
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
            throws RubyScriptException {
        for (final String classpath : classpathElements) {
            if (classpath.equals(jrubyJar.getAbsolutePath())) {
                return null;
            }
        }
        ClassRealm newClassRealm;
        try {
            ClassRealm jruby;
            try {
                jruby = classRealm.getWorld().getRealm("jruby");
            }
            catch (final NoSuchRealmException e) {
                jruby = classRealm.getParent().createChildRealm("jruby");
                jruby.addConstituent(jrubyJar.toURI().toURL());
            }
            try {
                jruby.getWorld().disposeRealm("pom");
            }
            catch (final NoSuchRealmException e) {
                // ignored
            }
            newClassRealm = jruby.createChildRealm("pom");
            for (final String classpath : classpathElements) {
                if (!classpath.contains("jruby-complete")) {
                    newClassRealm.addConstituent(new File(classpath).toURI()
                            .toURL());
                }
            }
        }
        catch (final DuplicateRealmException e) {
            throw new RubyScriptException("error in naming realms", e);
        }
        catch (final MalformedURLException e) {
            throw new RubyScriptException("hmm. found some malformed URL", e);
        }

        return newClassRealm;
    }

    @Override
    protected void doExecute(final File launchDirectory, final List<String> args,
            final File outputFile) throws RubyScriptException, IOException {
        doExecute(launchDirectory, outputFile, args, true);
    }

    private void doExecute(final File launchDirectory, final File outputFile,
            final List<String> args, final boolean warn)
            throws RubyScriptException, IOException {
        final String currentDir;
        if (launchDirectory != null) {
            currentDir = System.getProperty("user.dir");
            System.err.println("launch directory: "
                    + launchDirectory.getAbsolutePath());
            System.setProperty("user.dir", launchDirectory.getAbsolutePath());
        }
        else {
            currentDir = null;
        }

        args.addAll(0, this.factory.switches.list);

        if (warn) {
            if (this.factory.javaArgs.list.size() > 0) {
                this.logger.warn("have to ignore java arguments and properties in the current setup");
            }
            if (this.factory.env.size() > 0) {
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
                throw new RubyScriptException("some error in script " + args
                        + ": " + status);
            }

        }
        catch (final InstantiationException e) {
            throw new RubyScriptException(e);
        }
        catch (final IllegalAccessException e) {
            throw new RubyScriptException(e);
        }
        catch (final ClassNotFoundException e) {
            throw new RubyScriptException(e);
        }
        catch (final NoSuchMethodException e) {
            throw new RubyScriptException(e);
        }
        catch (final InvocationTargetException e) {
            throw new RubyScriptException(e);
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

    public void executeScript(final File launchDirectory, final String script,
            final List<String> args, final File outputFile)
            throws RubyScriptException, IOException {
        final StringBuilder buf = new StringBuilder();
        for (final Map.Entry<String, String> entry : this.factory.env.entrySet()) {
            buf.append("ENV['")
                    .append(entry.getKey())
                    .append("']='")
                    .append(entry.getValue())
                    .append("';");
        }
        buf.append(script);

        args.add(0, "-e");
        args.add(1, buf.toString());
        args.add(2, "--");
        doExecute(launchDirectory, outputFile, args, false);
    }
}
