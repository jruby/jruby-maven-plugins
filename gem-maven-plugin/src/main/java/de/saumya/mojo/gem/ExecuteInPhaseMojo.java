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
 * @goal execute_in_phase
 */
@Deprecated
public class ExecuteInPhaseMojo extends AbstractGemMojo {

    /** @parameter expression="${phase.file}" default-value="Mavenfile" */
    protected File   file = null;

    /** @parameter default-value="${phase.name}" */
    protected String phase   = null;

    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException {
        getLog().warn( "DEPRECATED: just do not use that anymore. use gem:exec instead" );
        this.factory.newScriptFromResource("maven/tools/execute_in_phase.rb")
                .addArg(this.file.getAbsolutePath())
                .addArg(this.phase)
                .executeIn(launchDirectory());
    }
}
