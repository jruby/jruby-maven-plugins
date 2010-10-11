package de.saumya.mojo.gem;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.script.ScriptException;

/**
 * goal to run "gem list".
 * 
 * @goal list
 */
public class ListMojo extends AbstractGemMojo {

    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException {
        this.factory.newScriptFromResource(GEM_RUBY_COMMAND)
                .addArg("list")
                .addArgs(this.args)
                .execute();
    }
}
