package de.saumya.mojo.rspec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.saumya.mojo.gem.AbstractGemMojo;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * executes the jruby command.
 * 
 * @goal test
 * @phase test
 * @requiresDependencyResolution test
 */
public class RSpecMojo extends AbstractGemMojo {

	/**
	 * The project base directory
	 * 
	 * @parameter expression="${basedir}"
	 * @required
	 * @readonly
	 */
	protected File basedir;

	/**
	 * The classpath elements of the project being tested.
	 * 
	 * @parameter expression="${project.testClasspathElements}"
	 * @required
	 * @readonly
	 */
	protected List<String> classpathElements;

	/**
	 * The flag to skip tests (optional, defaults to "false")
	 * 
	 * @parameter expression="${maven.test.skip}"
	 */
	protected boolean skipTests;

	/** @parameter default-value="${skipSpecs}" */
	protected boolean skipSpecs = false;

	/**
	 * The directory containing the RSpec source files
	 * 
	 * @parameter expression="spec"
	 */
	protected String specSourceDirectory;

	/**
	 * The directory where the RSpec report will be written to
	 * 
	 * @parameter expression="${basedir}/target"
	 * @required
	 */
	protected String outputDirectory;

	/**
	 * The name of the RSpec report (optional, defaults to "rspec-report.html")
	 * 
	 * @parameter expression="rspec-report.html"
	 */
	protected String reportName;

	/**
	 * List of system properties to set for the tests.
	 * 
	 * @parameter
	 */
	protected Properties systemProperties;

	private final RSpecScriptFactory rspecScriptFactory = new RSpecScriptFactory();

	private File specSourceDirectory() {
		return new File(launchDirectory(), this.specSourceDirectory);
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (this.skipTests || this.skipSpecs) {
			getLog().info("Skipping RSpec tests");
			return;
		} else {
			super.execute();
		}
	}

	@Override
	public void executeWithGems() throws MojoExecutionException,
			ScriptException, IOException {

		final File specSourceDirectory = specSourceDirectory();
		if (!specSourceDirectory.exists()) {
			getLog().info(
					"Skipping RSpec tests since " + specSourceDirectory
							+ " is missing");
			return;
		}
		getLog().info("Running RSpec tests from " + specSourceDirectory);

		final String reportPath = new File(this.outputDirectory,
				this.reportName).getAbsolutePath();

		initScriptFactory(this.rspecScriptFactory, reportPath);

		try {
			this.rspecScriptFactory.emit();
		} catch (final Exception e) {
			getLog().error("error emitting .rb", e);
		}

		this.factory.newScript(this.rspecScriptFactory.getScriptFile())
				.executeIn(launchDirectory());

		final File reportFile = new File(reportPath);

		Reader in = null;
		try {
			in = new FileReader(reportFile);
			final BufferedReader reader = new BufferedReader(in);

			String line = null;

			while ((line = reader.readLine()) != null) {
				if (line.contains("0 failures")) {
					return;
				}
			}
		} catch (final IOException e) {
			throw new MojoExecutionException(
					"Unable to read test report file: " + reportFile);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (final IOException e) {
					throw new MojoExecutionException(e.getMessage());
				}
			}
		}

		throw new MojoExecutionException("There were test failures");
	}

	private void initScriptFactory(final ScriptFactory factory,
			final String reportPath) {
		factory.setBaseDir(this.basedir.getAbsolutePath());
		factory.setOutputDir(new File(this.outputDirectory));
		factory.setReportPath(reportPath);
		factory.setSourceDir(specSourceDirectory().getAbsolutePath());
		Properties props = this.systemProperties;
		if (props == null) {
			props = new Properties();
		}
		factory.setSystemProperties(props);
	}

}
