/**
 * 
 */
package de.saumya.mojo.jruby;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.DuplicateRealmException;
import org.codehaus.classworlds.NoSuchRealmException;

class EmbeddedLauncher extends AbstractLauncher {

    private static final Class<?>[] No_ARG_TYPES = new Class[0];
    private final ClassRealm        classRealm;
    private static final Object[]   NO_ARGS      = new Object[0];

    EmbeddedLauncher(final Log log, final ClassRealm classRealm) {
        super(log);
        this.classRealm = classRealm;
    }

    public void execute(final File launchDirectory, final String[] args,
            final Set<Artifact> artifacts, final Artifact jrubyArtifact,
            final File classesDirectory, final File outputFile)
            throws MojoExecutionException,
            DependencyResolutionRequiredException {
        final ClassRealm jrubyClassRealm = cloneClassRealm(jrubyArtifact,
                                                           artifacts,
                                                           classesDirectory);
        final String currentDir = System.getProperty("user.dir");
        System.setProperty("user.dir", launchDirectory.getAbsolutePath());
        final PrintStream output = System.out;
        try {
            if (outputFile != null) {
                final PrintStream writer = new PrintStream(outputFile);
                System.setOut(writer);
            }
            // use reflection to avoid having jruby as plugin dependency
            Thread.currentThread()
                    .setContextClassLoader(jrubyClassRealm.getClassLoader());
            final Class<?> clazz = jrubyClassRealm.loadClass("org.jruby.Main");
            final Object main = clazz.newInstance();
            final Method m = clazz.getMethod("run", String[].class);
            final Object result = m.invoke(main, (Object) args);
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
                throw new MojoExecutionException("some error in script "
                        + Arrays.toString(args) + ": " + status);
            }

        }
        catch (final InstantiationException e) {
            throw new JRubyMainException(e);
        }
        catch (final IllegalAccessException e) {
            throw new JRubyMainException(e);
        }
        catch (final ClassNotFoundException e) {
            throw new JRubyMainException(e);
        }
        catch (final NoSuchMethodException e) {
            throw new JRubyMainException(e);
        }
        catch (final InvocationTargetException e) {
            throw new JRubyMainException(e);
        }
        catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
            System.setOut(output);
            if (currentDir != null) {
                System.setProperty("user.dir", currentDir);
            }
            try {
                this.classRealm.getWorld().disposeRealm("pom");
                jrubyClassRealm.setParent(null);
            }
            catch (final NoSuchRealmException e) {
            }

        }
    }

    @SuppressWarnings("serial")
    static class JRubyMainException extends MojoExecutionException {

        public JRubyMainException(final Exception cause) {
            super("error loading JRuby Main", cause);
        }

    }

    ClassRealm cloneClassRealm(final Artifact jrubyArtifact,
            final Set<Artifact> artifacts, final File classesDirectory)
            throws MojoExecutionException,
            DependencyResolutionRequiredException {
        ClassRealm newClassRealm;
        try {
            ClassRealm jruby;
            try {
                jruby = this.classRealm.getWorld().getRealm("jruby");
            }
            catch (final NoSuchRealmException e) {
                jruby = this.classRealm.getParent().createChildRealm("jruby");
                jruby.addConstituent(jrubyArtifact.getFile().toURI().toURL());
            }
            newClassRealm = jruby.createChildRealm("pom");
            newClassRealm.addConstituent(jrubyArtifact.getFile()
                    .toURI()
                    .toURL());
            for (final Artifact artifact : artifacts) {
                if (!artifact.getGroupId().equals("org.jruby")) {
                    newClassRealm.addConstituent(artifact.getFile()
                            .toURI()
                            .toURL());
                }
            }
            if (classesDirectory != null && classesDirectory.exists()) {
                newClassRealm.addConstituent(classesDirectory.toURI().toURL());
            }
        }
        catch (final DuplicateRealmException e) {
            throw new MojoExecutionException("error in naming realms", e);
        }
        catch (final MalformedURLException e) {
            throw new MojoExecutionException("hmm. found some malformed URL", e);
        }

        return newClassRealm;
    }

}