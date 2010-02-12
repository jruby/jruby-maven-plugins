package de.saumya.mojo.rails3;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * goal to run the rails database console
 * 
 * @goal dbconsole
 * @execute phase="initialize"
 */
public class DbConsoleMojo extends AbstractRailsMojo {

    /**
     * arguments for the database console command
     * 
     * @parameter default-value="${dbconsole}"
     */
    protected String dbconsoleArgs = null;

    @Override
    protected void executeWithGems() throws MojoExecutionException {
        String commandString = railsScript("dbconsole");
        if (this.args != null) {
            commandString += " " + this.args;
        }
        if (this.dbconsoleArgs != null) {
            commandString += " " + this.dbconsoleArgs;
        }
        execute(commandString, false);
    }
}
