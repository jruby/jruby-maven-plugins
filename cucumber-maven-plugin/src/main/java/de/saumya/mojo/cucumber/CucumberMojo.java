package de.saumya.mojo.cucumber;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.sonatype.aether.RepositorySystemSession;

import de.saumya.mojo.gem.AbstractGemMojo;
import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * maven wrapper around the cucumber command.
 * 
 * @goal test
 * @phase test
 * @requiresDependencyResolution test
 */
public class CucumberMojo extends AbstractGemMojo {

	/**
	 * cucumber features directory to be used for the cucumber command.
	 * 
	 * @parameter default-value="${cucumber.dir}"
	 */
	private final File cucumberDirectory = null;

	/**
	 * arguments for the cucumber command.
	 * 
	 * @parameter default-value="${cucumber.args}"
	 */
	private final String cucumberArgs = null;

	/**
	 * cucumber version used when there is no pom. defaults to latest version.
	 * 
	 * @parameter default-value="${cucumber.version}"
	 */
	private final String cucumberVersion = null;

	/** @parameter default-value="${project.build.directory}/surefire-reports" */
	private String testReportDirectory;

	/** @parameter default-value="${maven.test.skip}" */
	protected boolean skipTests = false;

	/** @parameter default-value="${skipCucumber}" */
	protected boolean skipCucumber = false;

	/**
	 * @parameter default-value="${repositorySystemSession}"
	 * @readonly
	 */
	private RepositorySystemSession repoSession;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (this.skipTests || this.skipCucumber) {
			getLog().info("Skipping Cucumber tests");
			return;
		} else {
			super.execute();
		}
	}

	@Override
	public void executeWithGems() throws MojoExecutionException,
			ScriptException, IOException, GemException {
        if (project.getBasedir() != null && !cucumberDirectory.exists() && this.args == null) {
            getLog().info("Skipping cucumber tests since " + cucumberDirectory + " is missing");
            return;
        }
        getLog().debug("Running Cucumber tests from " + cucumberDirectory);

        if (this.project.getBasedir() == null) {

			this.gemsInstaller.installGem("cucumber", this.cucumberVersion,
					this.repoSession, this.localRepository);

		}
		final Script script = this.factory.newScriptFromSearchPath("cucumber");
		script.addArg("-f", "pretty");
		if (this.project.getBasedir() != null) {
			script.addArg("-f", "junit");
			script.addArg("-o", this.testReportDirectory);
		}
		if (this.cucumberArgs != null) {
			script.addArgs(this.cucumberArgs);
		}
		if (this.args != null) {
			script.addArgs(this.args);
		}
		if (this.cucumberDirectory != null) {
			script.addArg(this.cucumberDirectory);
		}

		script.executeIn(launchDirectory());
	}
}
