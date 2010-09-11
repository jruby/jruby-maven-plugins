package de.saumya.mojo.rake;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.gem.AbstractGemMojo;
import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * maven wrapper around the rake command.
 * 
 * @goal rake
 * @requiresDependencyResolution test
 */
public class RakeMojo extends AbstractGemMojo {

    /**
     * rakefile to be used for the rake command.
     * 
     * @parameter default-value="${rake.file}"
     */
    private final File   rakefile    = null;

    /**
     * arguments for the rake command.
     * 
     * @parameter default-value="${rake.args}"
     */
    private final String rakeArgs    = null;

    /**
     * rake version used when there is no pom. defaults to 0.8.7
     * 
     * @parameter default-value="0.8.7" expression="${rake.version}"
     */
    private final String rakeVersion = null;

    // @Override
    // public void preExecute() throws MojoExecutionException,
    // MojoFailureException, IOException, ScriptException, GemException {
    // if (this.project.getBasedir() == null) {
    //
    // setupGems(this.manager.createGemArtifact("rake", this.rakeVersion));
    //
    // this.manager.addDefaultGemRepositoryForVersion(this.rakeVersion,
    // this.project.getRemoteArtifactRepositories());
    // }
    // }

    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException, GemException {
        if (this.project.getBasedir() == null) {

            setupGems(this.manager.createGemArtifact("rake", this.rakeVersion));

            this.manager.addDefaultGemRepositoryForVersion(this.rakeVersion,
                                                           this.project.getRemoteArtifactRepositories());
        }
        final Script script = this.factory.newScriptFromResource(RAKE_RUBY_COMMAND);
        script.addArg("-f", this.rakefile);

        if (this.rakeArgs != null) {
            script.addArgs(this.rakeArgs);
        }
        if (this.args != null) {
            script.addArgs(this.args);
        }

        script.executeIn(launchDirectory());
    }
}
