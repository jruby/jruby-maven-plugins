package de.saumya.mojo.gem;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import de.saumya.mojo.ruby.script.ScriptException;

/**
 * goal to run gem with the given arguments.
 *
 * DEPRECATED - DO NOT USE
 */
@Deprecated
@Mojo( name = "gem" )
public class GemMojo extends AbstractGemMojo {
    /**
     * arguments for the gem command of JRuby.
     */
    @Parameter( property = "gem.args" )
    protected String gemArgs = null;

    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException {
        getLog().warn( "DEPRECATED: just do not use that anymore. use gem:exec instead" );
        this.factory.newScriptFromJRubyJar("gem")
                .addArgs(this.gemArgs)
                .addArgs(this.args)
                .execute();
    }
}
