package de.saumya.mojo.cucumber;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.saumya.mojo.jruby.JRubyVersion;
import de.saumya.mojo.jruby.JRubyVersion.Mode;
import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.ruby.script.ScriptFactory;
import de.saumya.mojo.tests.AbstractTestMojo;
import de.saumya.mojo.tests.JRubyRun.Result;
import de.saumya.mojo.tests.TestResultManager;
import de.saumya.mojo.tests.TestScriptFactory;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * maven wrapper around the cucumber command.
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
@Deprecated
public class CucumberMojo extends AbstractTestMojo {

    enum ResultEnum {
        ERRORS, FAILURES, SKIPPED, TEST
    }

	/**
	 * cucumber features directory to be used for the cucumber command.
	 */
	@Parameter(property = "cucumber.dir", defaultValue = "features")
	private final File cucumberDirectory = null;

	/**
	 * arguments for the cucumber command.
	 */
	@Parameter(property = "cucumber.args")
	private final String cucumberArgs = null;

	@Parameter(property = "skipCucumber", defaultValue ="false")
	protected boolean skipCucumber = false;

    private TestResultManager resultManager;
    private File outputfile;
    
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		logger.warn("cucumber-maven-plugin is deprecated and is not maintained anymore");
		if (this.skip || this.skipTests || this.skipCucumber) {
			getLog().info("Skipping Cucumber tests");
		} 
		else {
	        if (this.project.getBasedir() != null && 
	                this.cucumberDirectory != null && !this.cucumberDirectory.exists() &&
	                this.args == null) {
	            getLog().info("Skipping cucumber tests since " + this.cucumberDirectory + " is missing");
	        }
	        else {
	            outputfile = new File(this.project.getBuild().getDirectory()
	                    .replace("${project.basedir}/", ""), "cucumber.txt");
	            if (outputfile.exists()){
	                outputfile.delete();
	            }
	            resultManager = new TestResultManager(summaryReport);
	            getLog().debug("Running Cucumber tests from " + this.cucumberDirectory);
	            super.execute();
	        }
		}
	}

    @Override
    protected TestScriptFactory newTestScriptFactory() {
        return new CucumberMavenTestScriptFactory();
    }
        
	@Override
    protected Result runIt(ScriptFactory factory, Mode mode, final JRubyVersion version, TestScriptFactory scriptFactory)
            throws IOException, ScriptException, MojoExecutionException {
	    scriptFactory.setSourceDir(new File("."));
        scriptFactory.emit();

		final Script script = factory.newScript(scriptFactory.getCoreScript());
		if (this.cucumberArgs != null) {
			script.addArgs(this.cucumberArgs);
		}
		if (this.args != null) {
			script.addArgs(this.args);
		}
		if (this.cucumberDirectory != null) {
			script.addArg(this.cucumberDirectory);
		}

        try {
            script.executeIn(launchDirectory());
        } catch (Exception e) {
            getLog().debug("exception in running tests", e);
        }

        return resultManager.generateReports(mode, version, outputfile);
	}
}
