package de.saumya.mojo.rails;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.script.ScriptException;

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
    protected void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException {
        this.factory.newScript(railsScriptFile("server"))
                .addArgs(this.serverArgs)
                .addArgs(this.args)
                .addArg("-e", this.env)
                .executeIn(launchDirectory());
    }
}
