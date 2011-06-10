package de.saumya.mojo.rails;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.script.ScriptException;

/**
 * goal to run rails rake with the given arguments.
 * 
 * @goal rake
 * @requiresDependencyResolution test
 */
public class RakeMojo extends AbstractRailsMojo {

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

    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException {
        this.factory.newScriptFromJRubyJar("rake")
                .addArgs(this.rakeArgs)
                .addArgs(this.args)
                .addArgs(this.task)
                .addArg("RAILS_ENV=" + this.env)
                .executeIn(launchDirectory());
    }
}
