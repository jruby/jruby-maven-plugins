package de.saumya.mojo.gem;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.script.ScriptException;

/**
 * Deprecated: just not really needed, use "gem:gem -Dargs=list" instead
 * goal to run "gem list".
 * 
 * @goal list
 */
@Deprecated
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
