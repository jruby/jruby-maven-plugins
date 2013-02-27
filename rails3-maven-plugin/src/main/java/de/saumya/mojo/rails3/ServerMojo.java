package de.saumya.mojo.rails3;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.script.ScriptException;

/**
 * goal to run the rails server.
 * 
 * @goal server
 * @requiresDependencyResolution runtime
 */
@Deprecated
public class ServerMojo extends AbstractRailsMojo {

    /**
     * arguments for the generate command
     * 
     * @parameter default-value="${server.args}"
     */
    protected String serverArgs = null;

    @Override
    protected void executeRails() throws MojoExecutionException,
            ScriptException, IOException {
        getLog().warn( "DEPRECATED: just do not use that anymore. use gem:exec or bundler:exec instead" );
        this.factory.newScript(railsScriptFile())
                .addArg("server")
                .addArgs(this.serverArgs)
                .addArgs(this.args)
                .addArg("-e", this.env)
                .executeIn(launchDirectory());
    }
}
