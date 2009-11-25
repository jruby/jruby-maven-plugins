/**
 * 
 */
package de.saumya.mojo.jruby;

import java.io.File;
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

    private final ClassRealm classRealm;

    EmbeddedLauncher(final Log log, final ClassRealm classRealm) {
        super(log);
        this.classRealm = classRealm;
    }

    public void execute(final File launchDirectory, final String[] args,
            final Set<Artifact> artifacts, final Artifact jrubyArtifact,
            final File classesDirectory) throws MojoExecutionException,
            DependencyResolutionRequiredException {
        final ClassRealm jrubyClassRealm = cloneClassRealm(jrubyArtifact,
                                                           artifacts,
                                                           classesDirectory);
        final String currentDir = System.getProperty("user.dir");
        System.setProperty("user.dir", launchDirectory.getAbsolutePath());
        try {
            Thread.currentThread()
                    .setContextClassLoader(jrubyClassRealm.getClassLoader());
            final Class<?> clazz = jrubyClassRealm.loadClass("org.jruby.Main");
            final Object main = clazz.newInstance();
            final Method m = clazz.getMethod("run", String[].class);
            final Integer result = (Integer) m.invoke(main, (Object) args);
            if (result.intValue() != 0) {
                throw new MojoExecutionException("some error in script "
                        + Arrays.toString(args) + ": " + result);
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
        finally {
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