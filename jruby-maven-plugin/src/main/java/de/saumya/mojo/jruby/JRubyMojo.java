package de.saumya.mojo.jruby;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * executes the jruby command.
 *
 * @goal jruby
 * @requiresDependencyResolution test
 */
public class JRubyMojo extends AbstractJRubyMojo {

    /**
     * arguments for the jruby command.
     * <br/>
     * Command line -Djruby.args=...
     *
     * @parameter expression="${jruby.args}"
     */
    protected String jrubyArgs = null;

    /**
     * ruby code which gets executed.
     * <br/>
     * Command line -Djruby.script=...
     *
     * @parameter expression="${jruby.script}"
     */
    protected String script = null;

    /**
     * ruby file which gets executed.
     * <br/>
     * Command line -Djruby.file=...
     *
     * @parameter expression="${jruby.file}"
     */
    protected File file = null;

    /**
     * ruby file found on search path which gets executed.
     * <br/>
     * Command line -Djruby.filename=...
     *
     * @parameter expression="${jruby.filename}"
     */
    protected String filename = null;

    /**
     * output file where the standard out will be written
     * <br/>
     * Command line -Djruby.outputFile=...
     *
     * @parameter expression="${jruby.outputFile}"
     */
    protected File outputFile = null;

    /**
     * directory of gem home to use when forking JRuby.
     * <br/>
     * Command line -Dgem.home=...
     *
     * @parameter expression="${gem.home}"
     *            default-value="${project.build.directory}/rubygems"
     */
    protected File          gemHome;

    /**
     * directory of JRuby path to use when forking JRuby.
     * <br/>
     * Command line -Dgem.path=...
     *
     * @parameter expression="${gem.path}"
     *            default-value="${project.build.directory}/rubygems"
     */
    protected File          gemPath;

    /**
     * use system gems instead of setting up GemPath/GemHome inside the build directory and ignores any set
     * gemHome and gemPath. you need to have both GEM_HOME and GEM_PATH environment variable set to make it work.
     * <br/>
     * Command line -Dgem.useSystem=...
     *
     * @parameter expression="${gem.useSystem}" default-value="false"
     */
    protected boolean          gemUseSystem;

    @Override
    public void executeJRuby() throws MojoExecutionException, IOException,
            ScriptException {
        if (gemHome != null && !gemUseSystem){
            factory.addEnv("GEM_HOME", this.gemHome);
        }
        if (gemPath != null && !gemUseSystem){
            factory.addEnv("GEM_PATH", this.gemPath);
        }
        Script s;
        if (this.script != null && this.script.length() > 0) {
            s = this.factory.newScript(this.script);
        } else if (this.file != null) {
            s = this.factory.newScript(this.file);
        } else if (this.filename != null) {
            s = this.factory.newScriptFromSearchPath( this.filename );
        } else {
            s = this.factory.newArguments();
        }
        s.addArgs(this.args);
        s.addArgs(this.jrubyArgs);
        if (s.isValid()) {
            if(outputFile != null){
                s.executeIn(launchDirectory(), outputFile);
            }
            else {
                s.executeIn(launchDirectory());
            }
        } else {
            getLog()
                    .warn(
                            "no arguments given. use -Dargs=... or -Djruby.script=... or -Djruby.file=...");
        }
    }
}