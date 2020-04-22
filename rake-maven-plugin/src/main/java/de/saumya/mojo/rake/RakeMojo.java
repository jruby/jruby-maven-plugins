package de.saumya.mojo.rake;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.gem.AbstractGemMojo;
import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * maven wrapper around the rake command.
 *
 * deprecated - use gem:exec or jruby9:exec with rake command instead
 */
@Deprecated
@Mojo(name = "rake", requiresDependencyResolution = ResolutionScope.TEST)
public class RakeMojo extends AbstractGemMojo {

    /**
     * rakefile to be used for the rake command.
     */
    @Parameter(property = "rake.file")
    private final File              rakefile    = null;

    /**
     * arguments for the rake command.
     */
    @Parameter(property = "rake.args")
    private final String            rakeArgs    = null;

    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException, GemException {
        logger.warn("rake-maven-plugin is deprecated and is not maintained anymore");
        final Script script = this.factory.newScriptFromJRubyJar("rake");
        if (this.rakefile != null){
            script.addArg("-f", this.rakefile);
        }
        
        if (this.rakeArgs != null) {
            script.addArgs(this.rakeArgs);
        }
        if (this.args != null) {
            script.addArgs(this.args);
        }

        script.executeIn(launchDirectory());
    }
}
