package de.saumya.mojo.rails;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal to run rails with build-in server.
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
        if (this.env != null) {
            command.append(" -e ").append(this.env);
        }
        execute(command.toString(), false);
    }
}
