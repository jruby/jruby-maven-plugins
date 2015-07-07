package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * executes a ruby script in context of the gems from pom. the arguments for
 * jruby are build like this:
 * <code>${jruby.args} ${exec.file} ${exec.args} ${args}</code> <br/>
 * to execute an inline script the exec parameters are ignored.
 */
@Mojo( name = "exec", defaultPhase = LifecyclePhase.INITIALIZE, 
       requiresDependencyResolution = ResolutionScope.TEST, requiresProject = false )
public class ExecMojo extends AbstractGemMojo {

    /**
     * ruby code from the pom configuration part which gets executed.
     */
    @Parameter( property = "exec.script" )
    protected String script   = null;

    /**
     * ruby file which gets executed in context of the given gems..
     */
    @Parameter( property = "exec.file" )
    protected File   file     = null;

    /**
     * ruby file found on search path which gets executed. the search path
     * includes the executable scripts which were installed via the given
     * gem-artifacts.
     */
    @Parameter( property = "exec.filename" )
    protected String filename = null;

    /**
     * output file where the standard out will be written
     */
    @Parameter( property = "exec.outputFile" )
    protected File outputFile = null;

    /**
     * arguments separated by whitespaces for the ruby script given through file parameter.
     * no quoting or escaping possible - if needed use execArglines instead.
     */
    @Parameter( property = "exec.args" )
    protected String execArgs = null;

    /**
     * an array of arguments which can contain spaces for the ruby script given through file parameter.
     */
    @Parameter
    protected String[] execArgLines = null;

    @Override
    protected void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException {
        Script s;
        if (this.script != null && this.script.length() > 0) {
            s = this.factory.newScript(this.script);
        }
        else if (this.file != null) {
            s = this.factory.newScript(this.file);
        }
        else if (this.filename != null) {
            s = this.factory.newScriptFromSearchPath( this.filename );
        }
        else {
            s = this.factory.newArguments();
        }
        if ( execArgLines != null ){
            for( String arg: execArgLines ){
                s.addArg( arg );
            }
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
