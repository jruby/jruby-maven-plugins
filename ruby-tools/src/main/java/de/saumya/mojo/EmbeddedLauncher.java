/**
 *
 */
package de.saumya.mojo;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.DuplicateRealmException;
import org.codehaus.classworlds.NoSuchRealmException;

class EmbeddedLauncher implements Launcher {

    private static final Class<?>[] No_ARG_TYPES = new Class[0];
    private static final Object[]   NO_ARGS      = new Object[0];

    private final ClassRealm        classRealm;
    // private final boolean verbose;
    private final File              gemHome;
    private final File              gemPath;

    EmbeddedLauncher(final boolean verbose,
            final List<String> classpathElements, final File jrubyGemHome,
            final File jrubyGemPath, final File jrubyJarFile,
            final ClassRealm classRealm) throws RubyScriptException {
        // this.verbose = verbose;
        this.gemHome = jrubyGemHome;
        this.gemPath = jrubyGemPath;
        this.classRealm = cloneClassRealm(jrubyJarFile,
                                          classpathElements,
                                          classRealm);
    }

    private void execute(final File outputFile, final List<String> args)
            throws RubyScriptException, IOException {
        // final String currentDir = System.getProperty("user.dir");
        // System.setProperty("user.dir", launchDirectory.getAbsolutePath());

        final PrintStream output = System.out;
        try {
            if (outputFile != null) {
                final PrintStream writer = new PrintStream(outputFile);
                System.setOut(writer);
                System.err.println("output file: " + outputFile);
            }
            System.err.println("args: " + args);

            // use reflection to avoid having jruby as plugin dependency
            Thread.currentThread()
                    .setContextClassLoader(this.classRealm.getClassLoader());
            final Class<?> clazz = this.classRealm.loadClass("org.jruby.Main");
            final Object main = clazz.newInstance();
            final Method m = clazz.getMethod("run", String[].class);
            final Object result = m.invoke(main,
                                           (Object) args.toArray(new String[args.size()]));
            final int status;
            if (result instanceof Integer) {
                // jruby before version 1.5
                status = ((Integer) result);
            }
            else {
                // jruby from version 1.5 onwards
                // TODO better error handling like error messages and . . . see
                // org.jruby.Main
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
            System.setOut(output);
            // if (currentDir != null) {
            // System.setProperty("user.dir", currentDir);
            // }
            // try {
            // this.classRealm.getWorld().disposeRealm("pom");
            // jrubyClassRealm.setParent(null);
            // }
            // catch (final NoSuchRealmException e) {
            // }

        }
    }

    private ClassRealm cloneClassRealm(final File jrubyJar,
            final List<String> classpathElements, final ClassRealm classRealm)
            throws RubyScriptException {
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
            newClassRealm = jruby.createChildRealm("pom");
            newClassRealm.addConstituent(jrubyJar.toURI().toURL());
            for (final String classpath : classpathElements) {
                newClassRealm.addConstituent(new File(classpath).toURI()
                        .toURL());
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

    public void execute(final String... args) throws RubyScriptException,
            IOException {
        execute(null, new ArrayList<String>(Arrays.asList(args)));
    }

    public void execute(final File outputFile, final String... args)
            throws RubyScriptException, IOException {
        execute(outputFile, new ArrayList<String>(Arrays.asList(args)));
    }

    public void executeScript(final String scriptName, final File outputFile,
            final String... args) throws RubyScriptException, IOException {
        final List<String> list = new ArrayList<String>();
        if (this.gemHome != null || this.gemPath != null) {
            if (this.gemHome != null) {
                final StringBuilder gems = new StringBuilder();
                gems.append("ENV['GEM_HOME']='")
                        .append(this.gemHome.getAbsolutePath())
                        .append("';");
                list.add(0, "-e");
                list.add(1, gems.toString());
            }
            if (this.gemPath != null) {
                final StringBuilder gems = new StringBuilder();
                gems.append("ENV['GEM_PATH']='")
                        .append(this.gemPath.getAbsolutePath())
                        .append("';");
                list.add(0, "-e");
                list.add(1, gems.toString());
            }
        }
        list.add("-e");
        final StringBuilder script = new StringBuilder();
        script.append("ARGV.clear;ARGV.<<([");
        boolean first = true;
        for (final String arg : args) {
            if (!first) {
                script.append(",");
            }
            else {
                first = false;
            }
            script.append("'").append(arg).append("'");
        }
        script.append("]).flatten!;load('")
                .append(scriptUri(scriptName))
                .append("');");
        list.add(script.toString());
        execute(outputFile, list);
    }

    public void executeScript(final String scriptName, final String... args)
            throws RubyScriptException, IOException {
        executeScript(scriptName, null, args);
    }

    private String scriptUri(final String scriptName) throws IOException {
        String script;
        final URL url = ScriptUtils.getScript(scriptName);
        if (url.getProtocol().equals("file")) {
            script = url.getPath();
        }
        else {
            script = url.toString();
        }
        return script;
    }
}
