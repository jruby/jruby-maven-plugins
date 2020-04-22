package de.saumya.mojo.bundler;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.gem.AbstractGemMojo;
import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * maven wrapper around the bundler update command.
 */
@Mojo(name = "update", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.TEST)
@Deprecated
public class UpdateMojo extends AbstractGemMojo {

    /**
     * arguments for the bundler command.
     */
    @Parameter(property = "bundler.args")
    private String            bundlerArgs;
    

    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException, GemException {
        logger.warn("bundler-maven-plugin is deprecated and is not maintained anymore");
        final Script script = this.factory.newScriptFromSearchPath("bundle");
        script.addArg("update");
        if (this.bundlerArgs != null) {
            script.addArgs(this.bundlerArgs);
        }
        if (this.args != null) {
            script.addArgs(this.args);
        }

        script.executeIn(launchDirectory());
    }
}
