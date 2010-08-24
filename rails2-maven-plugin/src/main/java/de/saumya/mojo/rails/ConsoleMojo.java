package de.saumya.mojo.rails;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.RubyScriptException;

/**
 * Goal to run rails console. it will ignore the fork parameter since forking
 * does not work with a console.
 * 
 * @goal console
 * @requiresDependencyResolution compile
 */
public class ConsoleMojo extends AbstractRailsMojo {

    /**
     * arguments for the console command
     * 
     * @parameter default-value="${console.args}"
     */
    protected String consoleArgs = null;

    @Override
    public void execute() throws MojoExecutionException {
        if (this.jrubyVersion != null
                && this.jrubyVersion.compareTo("1.5.0") < 0) {
            throw new MojoExecutionException("does not work with jruby version < 1.5.0");
        }
        // make sure the whole things run in the same process
        this.jrubyFork = false;
    }

    @Override
    public void executeWithGems() throws MojoExecutionException,
            RubyScriptException, IOException {
        this.factory.newScript(railsScriptFile("console"))
                .addArgs(this.consoleArgs)
                .addArgs(this.args)
                .addArg(this.env)
                .executeIn(launchDirectory());
    }
}
