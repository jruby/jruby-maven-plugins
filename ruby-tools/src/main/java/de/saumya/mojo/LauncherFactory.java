/**
 * 
 */
package de.saumya.mojo;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.codehaus.classworlds.ClassRealm;

public class LauncherFactory {

    private final Map<String, Launcher> launchers = new HashMap<String, Launcher>();

    public Launcher getForkedLauncher(final boolean verbose,
            final List<String> classpathElements, final File jrubyGemHome,
            final File jrubyGemPath, final File jrubyJarFile,
            final String jrubyLaunchMemory) {
        final TreeSet<String> key = new TreeSet<String>(classpathElements);
        key.add(jrubyGemHome.getAbsolutePath());
        key.add(jrubyGemPath.getAbsolutePath());
        key.add(jrubyJarFile.getAbsolutePath());
        key.add(jrubyLaunchMemory);
        final String k = key.toString();
        synchronized (this.launchers) {
            if (this.launchers.containsKey(k)) {
                return this.launchers.get(k);
            }
            else {
                final Launcher launcher = null;
                this.launchers.put(k, launcher);
                return launcher;
            }
        }
    }

    public Launcher getEmbeddedLauncher(final boolean verbose,
            final List<String> classpathElements, File jrubyGemHome,
            File jrubyGemPath, final File jrubyJarFile,
            final ClassRealm classRealm) throws RubyScriptException {
        final TreeSet<String> key = new TreeSet<String>(classpathElements);
        if (jrubyGemHome != null) {
            if (jrubyGemHome.exists()) {
                key.add(jrubyGemHome.getAbsolutePath());
            }
            else {
                jrubyGemHome = null;
            }
        }
        if (jrubyGemPath != null) {
            if (jrubyGemPath.exists()) {
                key.add(jrubyGemPath.getAbsolutePath());
            }
            else {
                jrubyGemPath = null;
            }
        }
        if (jrubyJarFile != null) {
            key.add(jrubyJarFile.getAbsolutePath());
        }
        key.add(classRealm.getId());
        final String k = key.toString();
        synchronized (this.launchers) {
            if (this.launchers.containsKey(k)) {
                return this.launchers.get(k);
            }
            else {
                final Launcher launcher = new EmbeddedLauncher(verbose,
                        classpathElements,
                        jrubyGemHome,
                        jrubyGemPath,
                        jrubyJarFile,
                        classRealm);
                this.launchers.put(k, launcher);
                return launcher;
            }
        }
    }

}