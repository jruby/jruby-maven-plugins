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
     * 
     * @parameter default-value="${irb.args}"
     */
    protected String  irbArgs = null;

    /**
     * launch IRB in a swing window.
     * 
     * @parameter default-value="${irb.swing}"
     */
    protected boolean swing   = false;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // make sure the whole things run in the same process
        super.jrubyFork = false;
        // // TODO jruby-complete tries to install gems into
        // //
        // file:/jruby-complete-1.5.1.jar!/META-INF/jruby.home/lib/ruby/gems/1.8
        // // instead of in $HOME/.gem or /usr/lib/ruby/1.8
        // this.includeOpenSSL = false;
        super.execute();
    }

    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException {
        this.factory.newScriptFromJRubyJar(this.swing
                ? IRB_SWING_RUBY_COMMAND
                : IRB_RUBY_COMMAND)
                .addArgs(this.irbArgs)
                .addArgs(this.args)
                .execute();
    }
}
