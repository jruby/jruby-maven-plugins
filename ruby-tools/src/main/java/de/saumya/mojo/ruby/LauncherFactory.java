/**
 * 
 */
package de.saumya.mojo.ruby;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface LauncherFactory {
    Launcher getLauncher(final boolean verbose,
            final List<String> classpathElements,
            final Map<String, String> env, final File jrubyJarFile,
            final String jrubyLaunchMemory) throws RubyScriptException;

}