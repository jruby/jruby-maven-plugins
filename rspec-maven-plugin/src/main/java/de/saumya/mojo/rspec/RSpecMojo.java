package de.saumya.mojo.rspec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.gem.AbstractGemMojo;

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

	private RSpecScriptFactory rspecScriptFactory = new RSpecScriptFactory();
	private ShellScriptFactory shellScriptFactory = new ShellScriptFactory();

	private File specSourceDirectory() {
		return new File(launchDirectory(), specSourceDirectory);
	}
	
	@Override
    public void execute() throws MojoExecutionException {
		if (skipTests) {
			getLog().info("Skipping RSpec tests");
			return;
		}
		
		super.execute();
	}

	@Override
	public void executeWithGems() throws MojoExecutionException {

		final File specSourceDirectory = specSourceDirectory();
		if (!specSourceDirectory.exists()) {
			getLog().info("Skipping RSpec tests since " + specSourceDirectory + " is missing");
			return;
		}
		getLog().info("Running RSpec tests from " + specSourceDirectory);

		String reportPath = new File(outputDirectory, reportName).getAbsolutePath();

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
		//execute(shellScriptFactory.getScriptFile().getPath());

		File reportFile = new File(reportPath);

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
		factory.setBaseDir(basedir.getAbsolutePath());
		factory.setClasspathElements(classpathElements);
		factory.setOutputDir(new File( outputDirectory) );
		factory.setReportPath(reportPath);
		factory.setSourceDir(specSourceDirectory().getAbsolutePath());
		factory.setGemHome( this.gemHome );
		factory.setGemPath( this.gemPath );
		Properties props = systemProperties;
		if (props == null) {
			props = new Properties();
		}
		factory.setSystemProperties(props);
	}

}
