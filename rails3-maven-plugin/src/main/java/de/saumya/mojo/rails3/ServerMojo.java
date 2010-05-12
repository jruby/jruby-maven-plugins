package de.saumya.mojo.rails3;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * goal to run the rails server.
 * 
 * @goal server
 * @requiresDependencyResolution compile
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
        final StringBuilder command = railsScript("server");
        if (this.serverArgs != null) {
            command.append(" ").append(this.serverArgs);
        }
        if (this.args != null) {
            command.append(" ").append(this.args);
        }
        // TODO verify this
        if (this.environment != null) {
            command.append(" -e ").append(this.environment);
        }
        execute(command.toString(), false);
    }
}
