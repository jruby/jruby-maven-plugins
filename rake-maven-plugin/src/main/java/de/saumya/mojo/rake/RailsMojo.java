package de.saumya.mojo.rake;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.gem.AbstractGemMojo;
import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * goal to run rails rake with the given arguments.
 * 
 * @goal rails
 * @requiresDependencyResolution test
 */
public class ailsMojo  extends AbstractGemMojo {

    /**
     * arguments for the generate command
     * 
     * @parameter default-value="${rake.args}"
     */
    protected String rakeArgs = null;

    /**
     * the path to the application to be generated
     * 
     * @parameter default-value="${task}"
     */
    protected String task     = null;
    
    /**
     * either development or test or production or whatever else is possible
     * with your config
     * 
     * @parameter expression="${rails.env}"
     */
    protected String                  env;
    
    @Override
    public void executeWithGems() throws MojoExecutionException, ScriptException,
            IOException, GemException {    
        final Script script = factory.newScriptFromSearchPath("rake");
        script.addArgs(task);
        if(env != null && env.trim().length() > 0){
            script.addArg("RAILS_ENV=" + env);
        }
        if (this.rakeArgs != null) {
            script.addArgs(this.rakeArgs);
        }
        if (this.args != null) {
            script.addArgs(this.args);
        }
        script.executeIn(launchDirectory());
    }
}
