/**
 * 
 */
package de.saumya.mojo.ruby;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.codehaus.classworlds.ClassRealm;

public class LauncherFactoryOld {

    private Launcher forked;
    private Launcher embedded;

    public synchronized Launcher getForkedLauncher(final boolean verbose,
            final List<String> classpathElements,
            final Map<String, String> env, final File jrubyJarFile,
            final String jrubyLaunchMemory) {
        if (this.forked == null) {
            this.forked = null;
        }
        return this.forked;
    }

    public synchronized Launcher getEmbeddedLauncher(final boolean verbose,
            final List<String> classpathElements,
            final Map<String, String> env, final File jrubyJarFile,
            final ClassRealm classRealm) throws RubyScriptException {
        if (this.embedded == null) {
            this.embedded = new EmbeddedLauncher(verbose,
                    classpathElements,
                    env,
                    jrubyJarFile,
                    classRealm);
        }
        return this.embedded;
    }

}
