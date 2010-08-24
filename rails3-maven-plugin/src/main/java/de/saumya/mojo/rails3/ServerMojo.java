package de.saumya.mojo.rails3;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.RubyScriptException;

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
    protected void executeRails() throws MojoExecutionException,
            RubyScriptException, IOException {
        this.factory.newScript(railsScriptFile())
                .addArg("server")
                .addArgs(this.serverArgs)
                .addArgs(this.args)
                .addArg("-e", this.env)
                .executeIn(launchDirectory());
    }
}
