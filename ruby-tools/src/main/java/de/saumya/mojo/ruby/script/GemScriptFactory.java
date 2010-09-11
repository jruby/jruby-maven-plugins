/**
 * 
 */
package de.saumya.mojo.ruby.script;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.codehaus.classworlds.ClassRealm;

import de.saumya.mojo.ruby.Logger;

public class GemScriptFactory extends ScriptFactory {// implements GemService {

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
    //
    // public File binDirectory() throws ScriptException {
    // if (this.gemHome == null) {
    // if (System.getenv(GEM_HOME) == null) {
    // throw new ScriptException("no GEM_HOME set");
    // }
    // else {
    // return new File(System.getenv(GEM_HOME), "bin");
    // }
    // }
    // else {
    // return new File(this.gemHome, "bin");
    // }
    // }
    //
    // public File gemDirectory() throws ScriptException {
    // if (this.gemPath == null) {
    // if (System.getenv(GEM_PATH) == null) {
    // throw new ScriptException("no GEM_PATH set");
    // }
    // else {
    // return new File(System.getenv(GEM_PATH), "gems");
    // }
    // }
    // else {
    // return new File(this.gemPath, "gems");
    // }
    // }
    //
    // public File binScriptFile(final String script) throws ScriptException {
    // return new File(binDirectory(), script);
    // }
    //
    // public String binScript(final String script) throws ScriptException {
    // return binScriptFile(script).getAbsolutePath();
    // }
}