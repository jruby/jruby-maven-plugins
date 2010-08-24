/**
 * 
 */
package de.saumya.mojo.ruby;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.codehaus.classworlds.ClassRealm;

public class GemScriptFactory extends ScriptFactory implements GemService {

    public static final String GEM_HOME = "GEM_HOME";
    public static final String GEM_PATH = "GEM_PATH";

    private final File         gemHome;
    private final File         gemPath;

    public GemScriptFactory(final Logger logger, final ClassRealm classRealm,
            final File jrubyJar, final List<String> classpathElements,
            final boolean fork, final File gemHome, final File gemPath)
            throws RubyScriptException, IOException {
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

    public File binDirectory() throws RubyScriptException {
        if (this.gemHome == null) {
            if (System.getenv(GEM_HOME) == null) {
                throw new RubyScriptException("no GEM_HOME set");
            }
            else {
                return new File(System.getenv(GEM_HOME), "bin");
            }
        }
        else {
            return new File(this.gemHome, "bin");
        }
    }

    public File gemDirectory() throws RubyScriptException {
        if (this.gemPath == null) {
            if (System.getenv(GEM_PATH) == null) {
                throw new RubyScriptException("no GEM_PATH set");
            }
            else {
                return new File(System.getenv(GEM_PATH), "gems");
            }
        }
        else {
            return new File(this.gemPath, "gems");
        }
    }

    public File binScriptFile(final String script) throws RubyScriptException {
        return new File(binDirectory(), script);
    }

    public String binScript(final String script) throws RubyScriptException {
        return binScriptFile(script).getAbsolutePath();
    }
}