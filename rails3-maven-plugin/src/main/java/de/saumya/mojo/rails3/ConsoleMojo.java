package de.saumya.mojo.rails3;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.saumya.mojo.ruby.script.ScriptException;

/**
 * goal to run the rails console. it will ignore the fork parameter since
 * forking does not work with a console.
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
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (getJrubyVersion().toString().compareTo("1.5.0") < 0) {
            throw new MojoExecutionException("does not work with jruby version < 1.5.0");
        }
        // make sure the whole things run in the same process
        this.jrubyFork = false;
        super.execute();
    }

    @Override
    public void executeRails() throws MojoExecutionException,
            ScriptException, IOException {
        this.factory.newScript(railsScriptFile())
                .addArg("console")
                .addArgs(this.consoleArgs)
                .addArgs(this.args)
                .addArg(this.env)
                .executeIn(launchDirectory());
    }
}
