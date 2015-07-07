package de.saumya.mojo.gem;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import de.saumya.mojo.ruby.script.ScriptException;

/**
 * maven wrapper around IRB.
 * <br/>
 * DEPRECATED - DO NOT USE
 */
@Deprecated
@Mojo( name = "irb", requiresDependencyResolution = ResolutionScope.TEST )
public class IrbMojo extends AbstractGemMojo {

    /**
     * arguments for the irb command.
     */
    @Parameter( property = "irb.args" )
    protected String irbArgs = null;

    /**
     * launch IRB in a swing window.
     */
    @Parameter( property = "irb.swing", defaultValue = "false" )
    protected boolean swing;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // make sure the whole things run in the same process
        super.jrubyFork = false;
        // this.includeOpenSSL = false;
        super.execute();
    }

    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException {
        this.factory.newScriptFromJRubyJar(this.swing
                ? "jirb_swing"
                : "jirb")
                .addArgs(this.irbArgs)
                .addArgs(this.args)
                .execute();
    }
}
