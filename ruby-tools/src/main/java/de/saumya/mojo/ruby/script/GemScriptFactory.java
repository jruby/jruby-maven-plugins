/**
 * 
 */
package de.saumya.mojo.ruby.script;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.classworlds.ClassRealm;

import de.saumya.mojo.ruby.Logger;
import de.saumya.mojo.ruby.gems.GemsConfig;

public class GemScriptFactory extends ScriptFactory {

    public static final String GEM_HOME = "GEM_HOME";
    public static final String GEM_PATH = "GEM_PATH";

    private final GemsConfig   gemsConfig;

    public GemScriptFactory(final Logger logger, final ClassRealm classRealm,
            final File jrubyJar, final List<String> classpathElements,
            final boolean fork, final GemsConfig config)
            throws ScriptException, IOException {
        super(logger, classRealm, jrubyJar, classpathElements, fork);
        this.gemsConfig = config;
    }

    @Override
    public Map<String, String> environment() {
        Map<String, String> result = new HashMap<String, String>(super.environment());
        if (this.gemsConfig.getGemHome() != null) {
            result.put(GEM_HOME, this.gemsConfig.getGemHome()
                    .getAbsolutePath()
                    .replaceFirst("/[$][{]project.basedir[}]/", "/"));
        }

        if (this.gemsConfig.getGemPath().length > 0) {
            StringBuilder paths = new StringBuilder();
            for (File path : this.gemsConfig.getGemPath()) {
                if (paths.length() > 0) {
                    paths.append(System.getProperty("path.separator"));
                }
                paths.append(path.getAbsolutePath()
                        .replaceFirst("/[$][{]project.basedir[}]/", "/"));
            }

            result.put(GEM_PATH, paths.toString());
        }

        return result;
    }

    @Override
    public Script newScriptFromSearchPath(final String scriptName)
            throws IOException {
        final File script = new File(gemsConfig.getBinDirectory(), scriptName);
        if (script.exists()) {
            return newScript(script.getAbsoluteFile());
        }
        else {
            return super.newScriptFromSearchPath(scriptName);
        }
    }
}