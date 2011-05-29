package de.saumya.mojo.gem;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.saumya.mojo.ruby.script.ScriptException;

/**
 * maven wrpper around IRB.
 * 
 * @goal irb
 * @requiresDependencyResolution test
 */
public class IrbMojo extends AbstractGemMojo {

    /**
     * arguments for the irb command.
     * <br/>
     * Command line -Dirb.args=...
     * 
     * @parameter expression="${irb.args}"
     */
    protected String irbArgs = null;

    /**
     * launch IRB in a swing window.
     * 
     * @parameter default-value="false" expression="${irb.swing}"
     * <br/>
     * Command line -Dirb.swing=...
     */
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
