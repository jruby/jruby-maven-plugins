package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * executes a ruby script in context of the gems from pom. the arguments for
 * jruby are build like this:
 * <code>${jruby.args} ${exec.file} ${exec.args} ${args}</code> <br/>
 * to execute an inline script the exec parameters are ignored.
 * 
 * @goal exec
 * @requiresDependencyResolution test
 * @execute phase="process-resources"
 */
public class ExecMojo extends AbstractGemMojo {

    /**
     * ruby code from the pom configuration part which gets executed.
     * <br/>
     * Command line -Dexec.script=...
     * 
     * @parameter expression="${exec.script}"
     */
    protected String script   = null;

    /**
     * ruby file which gets executed in context of the given gems..
     * <br/>
     * Command line -Dexec.file=...
     * 
     * @parameter expression="${exec.file}"
     */
    protected File   file     = null;

    /**
     * output file where the standard out will be written
     * <br/>
     * Command line -Dexec.outputFile=...
     * 
     * @parameter expression="${exec.outputFile}"
     */
    protected File outputFile = null;

    /**
     * arguments for the ruby script given through file parameter.
     * <br/>
     * Command line -Dexec.args=...
     * 
     * @parameter expression="${exec.args}"
     */
    protected String execArgs = null;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        this.includeOpenSSL = this.jrubyFork;
        super.execute();
    }

    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException {
        Script s;
        if (this.script != null && this.script.length() > 0) {
            s = this.factory.newScript(this.script);
        }
        else if (this.file != null) {
            s = this.factory.newScript(this.file);
        }
        else {
            s = this.factory.newArguments();
        }
        s.addArgs(this.execArgs);
        s.addArgs(this.args);
        if (s.isValid()) { 
            if(outputFile != null){
                s.executeIn(launchDirectory(), outputFile);
            }
            else {
                s.executeIn(launchDirectory());
            }
        }
        else {
            getLog().warn("no arguments given. use -Dexec.script=... or -Dexec.file=...");
        }
    }
}
