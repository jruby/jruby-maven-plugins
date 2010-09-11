package de.saumya.mojo.gem;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.jruby.AbstractJRubyMojo;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * goal to run "gem spec".
 * 
 * @goal spec
 */
public class SpecMojo extends AbstractJRubyMojo {
    /**
     * arguments for the gem command of JRuby.
     * 
     * @parameter default-value="${gemfile}"
     */
    protected String gemfile = null;

    @Override
    public void executeJRuby() throws MojoExecutionException,
            ScriptException, IOException {
        if (this.gemfile == null) {
            getLog().warn("please specifiy a gem file, use '-Dgemfile=...'");
        }
        else {
            this.factory.newScriptFromResource(AbstractGemMojo.GEM_RUBY_COMMAND)
                    .addArg("spec")
                    .addArgs(this.gemfile)
                    .addArgs(this.args)
                    .execute();
        }
    }
}
