package de.saumya.mojo.gem;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.RubyScriptException;

/**
 * goal to run gem with the given arguments.
 * 
 * @goal gem
 */
public class GemMojo extends AbstractGemMojo {
    /**
     * arguments for the gem command of JRuby.
     * 
     * @parameter default-value="${gem.args}"
     */
    protected String gemArgs = null;

    @Override
    public void executeWithGems() throws MojoExecutionException,
            RubyScriptException, IOException {
        this.factory.newScriptFromResource(GEM_RUBY_COMMAND)
                .addArgs(this.gemArgs)
                .addArgs(this.args)
                .execute();
    }
}
