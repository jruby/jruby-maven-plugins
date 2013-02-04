package de.saumya.mojo.rails3;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.script.ScriptException;

/**
 * goal to run the rails database console
 * 
 * @goal dbconsole
 * @requiresDependencyResolution compile
 */
public class DbConsoleMojo extends AbstractRailsMojo {

    /**
     * arguments for the database console command
     * 
     * @parameter default-value="${dbconsole.args}"
     */
    protected String dbconsoleArgs = null;

    @Override
    public void execute() throws MojoExecutionException {
        if (getJrubyVersion().toString().compareTo("1.5.0") < 0) {
            throw new MojoExecutionException("does not work with jruby version < 1.5.0");
        }
        // make sure the whole things run in the same process
        this.jrubyFork = false;
    }

    @Override
    public void executeRails() throws MojoExecutionException,
            ScriptException, IOException {
        this.factory.newScript(railsScriptFile())
                .addArg("dbconsole")
                .addArgs(this.dbconsoleArgs)
                .addArgs(this.args)
                .addArg(this.env)
                .executeIn(launchDirectory());
    }
}
