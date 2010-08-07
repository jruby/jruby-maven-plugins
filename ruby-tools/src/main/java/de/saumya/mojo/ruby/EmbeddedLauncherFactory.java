/**
 * 
 */
package de.saumya.mojo.ruby;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;

@Component(role = LauncherFactory.class, hint = "embedded")
public class EmbeddedLauncherFactory implements LauncherFactory {

    private Launcher embedded;

    public synchronized Launcher getLauncher(final boolean verbose,
            final List<String> classpathElements,
            final Map<String, String> env, final File jrubyJarFile,
            final String jrubyLaunchMemory) throws RubyScriptException {
        if (this.embedded == null) {
            this.embedded = new EmbeddedLauncher(verbose,
                    classpathElements,
                    env,
                    jrubyJarFile,
                    null);
        }
        return this.embedded;
    }

}