package de.saumya.mojo.jruby;

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
     * @parameter default-value="${irb.args}"
     */
    protected String irbArgs = null;

    /**
     * launch IRB in a swing window.
     * 
     * @parameter default-value="${irb.swing}"
     */
    protected boolean swing = false;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // make sure the whole things run in the same process
        super.jrubyFork = false;
        super.execute();
    }

    @Override
    public void executeJRuby() throws MojoExecutionException, ScriptException,
            IOException {
        this.factory.newScriptFromJRubyJar(
                this.swing ? IRB_SWING_RUBY_COMMAND : IRB_RUBY_COMMAND)
                .addArgs(this.irbArgs).addArgs(this.args).execute();
    }
}
