package de.saumya.mojo.jruby;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.RubyScriptException;

/**
 * maven wrpper around IRB.
 * 
 * @goal irb
 * @requiresDependencyResolution test
 */
public class IRBMojo extends AbstractJRubyMojo {

    // override super mojo and make this readonly
    /**
     * @parameter expression="false"
     * @readonly
     */
    protected boolean fork;

    /**
     * arguments for the irb command.
     * 
     * @parameter default-value="${jruby.irb.args}"
     */
    protected String irbArgs = null;

    /**
     * launch IRB in a swing window.
     * 
     * @parameter default-value="${gem.irb.swing}"
     */
    protected boolean swing = false;

    @Override
    public void executeJRuby() throws MojoExecutionException,
            RubyScriptException, IOException {
        // make sure the whole things run in the same process
        super.jrubyFork = false;
        this.factory.newScriptFromResource(
                this.swing ? IRB_SWING_RUBY_COMMAND : IRB_RUBY_COMMAND)
                .addArgs(this.irbArgs).addArgs(this.args).execute();
    }
}
