package de.saumya.mojo.rails3;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * goal to run the rails console
 * 
 * @goal console
 * @execute phase="initialize"
 */
public class ConsoleMojo extends AbstractRailsMojo {

    /**
     * arguments for the console command
     * 
     * @parameter default-value="${console}"
     */
    protected String consoleArgs = null;

    @Override
    protected void executeWithGems() throws MojoExecutionException {
        String commandString = railsScript("console");
        if (this.args != null) {
            commandString += " " + this.args;
        }
        if (this.consoleArgs != null) {
            commandString += " " + this.consoleArgs;
        }
        execute(commandString, false);
    }
}
