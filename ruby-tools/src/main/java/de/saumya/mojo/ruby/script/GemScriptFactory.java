/**
 * 
 */
package de.saumya.mojo.ruby.script;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.codehaus.classworlds.ClassRealm;

import de.saumya.mojo.ruby.Logger;

public class GemScriptFactory extends ScriptFactory {

    public static final String GEM_HOME = "GEM_HOME";
    public static final String GEM_PATH = "GEM_PATH";

    private final File         gemHome;
    private final File         gemPath;

    public GemScriptFactory(final Logger logger, final ClassRealm classRealm,
            final File jrubyJar, final List<String> classpathElements,
            final boolean fork, final File gemHome, final File gemPath)
            throws ScriptException, IOException {
        super(logger, classRealm, jrubyJar, classpathElements, fork);

        this.gemHome = new File(gemHome.getAbsolutePath()
                .replaceFirst(".*/[$][{]project.basedir[}]/", ""));
        if (this.gemHome != null) {
            addEnv(GEM_HOME, this.gemHome.getPath());
        }
        this.gemPath = new File(gemPath.getAbsolutePath()
                .replaceFirst(".*/[$][{]project.basedir[}]/", ""));
        if (this.gemPath != null) {
            addEnv(GEM_PATH, this.gemPath.getPath());
        }
    }

    @Override
    public Script newScriptFromSearchPath(final String scriptName)
            throws IOException {
        final File script = new File(new File(this.gemHome, "bin"), scriptName);
        if (script.exists()) {
            return newScript(script.getAbsoluteFile());
        }
        else {
            return super.newScriptFromSearchPath(scriptName);
        }
    }
}