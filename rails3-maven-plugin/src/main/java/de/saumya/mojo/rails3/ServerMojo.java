package de.saumya.mojo.rails3;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * goal to run the rails server.
 * 
 * @goal server
 * @execute phase="initialize"
 */
public class ServerMojo extends AbstractRailsMojo {

    /**
     * arguments for the generate command
     * 
     * @parameter default-value="${server.args}"
     */
    protected String serverArgs = null;

    @Override
    protected void executeWithGems() throws MojoExecutionException {
        String commandString = railsScript("server");
        if (this.serverArgs != null) {
            commandString += " " + this.serverArgs;
        }
        if (this.args != null) {
            commandString += " " + this.args;
        }
        execute(commandString, false);
    }
}
