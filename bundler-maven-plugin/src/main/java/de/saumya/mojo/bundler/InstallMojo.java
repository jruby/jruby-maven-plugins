package de.saumya.mojo.bundler;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.sonatype.aether.RepositorySystemSession;

import de.saumya.mojo.gem.AbstractGemMojo;
import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * maven wrapper around the bundler command.
 * 
 * @goal install
 * @phase initialize
 * @requiresDependencyResolution test
 */
public class InstallMojo extends AbstractGemMojo {

    /**
     * arguments for the bundler command.
     * 
     * @parameter default-value="${bundler.args}"
     */
    private final String bundlerArgs = null;

    /**
     * bundler version used when there is no pom. defaults to latest version.
     * 
     * @parameter default-value="${bundler.version}"
     */
    private final String bundlerVersion = null;

    /**
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    private RepositorySystemSession repoSession;

    @Override
    public void executeWithGems() throws MojoExecutionException, 
					 ScriptException, 
					 IOException, 
					 GemException {
	final Script script = this.factory.newScriptFromSearchPath("bundle");
	script.addArg("install");
	if (this.project.getBasedir() == null) {

	    this.gemsInstaller.installGem("bundler", 
					  this.bundlerVersion,
					  this.repoSession, 
					  this.localRepository);

	}
	else {
	    script.addArg("--quiet");
	    script.addArg("--local");
	}
	if (this.bundlerArgs != null) {
	    script.addArgs(this.bundlerArgs);
	}
	if (this.args != null) {
	    script.addArgs(this.args);
	}

	script.executeIn(launchDirectory());
    }
}
