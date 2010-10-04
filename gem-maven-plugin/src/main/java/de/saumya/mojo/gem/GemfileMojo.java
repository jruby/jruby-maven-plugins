package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.script.ScriptException;

/**
 * the mojo allows to embed a plugin inside a Gemfile (from bundler) and execute
 * it during the specified phase. this primary meant for the Gemfile DSL
 * extension for polyglot maven which can use a Gemfile as POM.
 * 
 * @goal gemfile
 */
public class GemfileMojo extends AbstractGemMojo {

    /** @parameter expression="${gem.file}" default-value="Gemfile" */
    protected File   gemFile = null;

    /** @parameter default-value="${gem.phase}" */
    protected String phase   = null;

    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException {
        this.factory.newScriptFromResource("maven/tools/gemfile_execute_phase.rb")
                .addArg(this.gemFile.getAbsolutePath())
                .addArg(this.phase)
                .executeIn(launchDirectory());
    }
}
