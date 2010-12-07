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

        if (gemHome != null) {
            this.gemHome = new File(gemHome.getAbsolutePath()
                                    .replaceFirst("/[$][{]project.basedir[}]/", "/"));
                                addEnv(GEM_HOME, this.gemHome.getPath());
        }
        else{
            this.gemHome = null;            
        }
        
        if (gemPath != null) {
          this.gemPath = new File(gemPath.getAbsolutePath()
          .replaceFirst("/[$][{]project.basedir[}]/", "/"));
            addEnv(GEM_PATH, this.gemPath.getPath());
        }
        else {
            this.gemPath = null;
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