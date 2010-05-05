package de.saumya.mojo.rspec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.saumya.mojo.jruby.AbstractJRubyMojo;

/**
 * executes the jruby command.
 * 
 * @goal test
 * @requiresDependencyResolution test
 */
public class RSpecMojo extends AbstractJRubyMojo {

	/**
	 * The project base directory
	 * 
	 * @parameter expression="${basedir}"
	 * @required
	 * @readonly
	 */
	protected String basedir;

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

	/**
	 * The directory containing the RSpec source files
	 * 
	 * @parameter expression="${basedir}/specs/"
	 */
	protected String specSourceDirectory;

	/**
	 * The directory where the RSpec report will be written to
	 * 
	 * @parameter expression="target/"
	 * @required
	 */
	protected String outputDirectory;

	/**
	 * The name of the RSpec report (optional, defaults to "rspec_report.html")
	 * 
	 * @parameter expression="rspec_report.html"
	 */
	protected String reportName;

	/**
	 * List of system properties to set for the tests.
	 * 
	 * @parameter
	 */
	protected Properties systemProperties;

	private RSpecScriptFactory rspecScriptFactory = new RSpecScriptFactory();
	private ShellScriptFactory shellScriptFactory = new ShellScriptFactory();

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skipTests) {
			getLog().info("Skipping RSpec tests");
			return;
		}

		if (!new File(specSourceDirectory).exists()) {
			getLog().info("Skipping RSpec tests since " + specSourceDirectory + " is missing");
			return;
		}
		getLog().info("Running RSpec tests from " + specSourceDirectory);

		String reportPath = new File(outputDirectory, reportName).getPath();

		initScriptFactory(rspecScriptFactory, reportPath);
		initScriptFactory(shellScriptFactory, reportPath);

		try {
			rspecScriptFactory.emit();
		} catch (Exception e) {
			getLog().error("error emitting .rb", e);
		}
		try {
			shellScriptFactory.emit();
		} catch (Exception e) {
			getLog().error("error emitting .sh", e);
		}

		execute(rspecScriptFactory.getScriptFile().getPath());

		File reportFile = new File(this.project.getBasedir(), reportPath);
		Reader in = null;
		try {
			in = new FileReader(reportFile);
			BufferedReader reader = new BufferedReader(in);

			String line = null;

			while ((line = reader.readLine()) != null) {
				if (line.contains("0 failures")) {
					return;
				}
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to read test report file: " + reportFile);
		} finally {
			if ( in != null ) {
				try {
					in.close();
				} catch (IOException e) {
					throw new MojoExecutionException( e.getMessage() );
				}
			}
		}
		

		throw new MojoExecutionException("There were test failures");
	}

	private void initScriptFactory(ScriptFactory factory, String reportPath) {
		factory.setBaseDir(basedir);
		factory.setClasspathElements(classpathElements);
		factory.setOutputDir(new File(basedir, outputDirectory));
		factory.setReportPath(reportPath);
		factory.setSourceDir(specSourceDirectory);
		Properties props = systemProperties;
		if (props == null) {
			props = new Properties();
		}
		factory.setSystemProperties(props);
	}

}
