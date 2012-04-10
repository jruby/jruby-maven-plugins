package de.saumya.mojo.cucumber;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.ruby.script.ScriptFactory;
import de.saumya.mojo.runit.AbstractTestMojo;
import de.saumya.mojo.runit.JRubyRun.Mode;
import de.saumya.mojo.runit.JRubyRun.Result;
import de.saumya.mojo.runit.TestScriptFactory;

/**
 * maven wrapper around the cucumber command.
 * 
 * @goal test
 * @phase test
 * @requiresDependencyResolution test
 */
public class CucumberMojo extends AbstractTestMojo {

    enum ResultEnum {
        ERRORS, FAILURES, TEST
    }

	/**
	 * cucumber features directory to be used for the cucumber command.
	 * 
	 * @parameter expression="${cucumber.dir}" 
	 */
	private final File cucumberDirectory = null;

	/**
	 * arguments for the cucumber command.
	 * 
	 * @parameter default-value="${cucumber.args}"
	 */
	private final String cucumberArgs = null;

//	/**
//	 * cucumber version used when there is no pom. defaults to latest version.
//	 * 
//	 * @parameter default-value="${cucumber.version}"
//	 */
//	private final String cucumberVersion = null;

	/** @parameter default-value="${skipCucumber}" */
	protected boolean skipCucumber = false;

//	/**
//	 * @parameter default-value="${repositorySystemSession}"
//	 * @readonly
//	 */
//	private RepositorySystemSession repoSession;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (this.skipTests || this.skipCucumber) {
			getLog().info("Skipping Cucumber tests");
			return;
		} else {
	        if (this.project.getBasedir() != null && 
	                ((this.cucumberDirectory != null && !this.cucumberDirectory.exists()) ||  
	                (this.cucumberDirectory == null && !new File(this.project.getBasedir(), "features").exists())) &&
	                this.args == null) {
	            getLog().info("Skipping cucumber tests since " + this.cucumberDirectory + " is missing");
	            return;
	        }
	        getLog().debug("Running Cucumber tests from " + this.cucumberDirectory);
			super.execute();
		}
	}

    @Override
    protected TestScriptFactory newTestScriptFactory(Mode mode) {
        return new CucumberMavenTestScriptFactory();
    }
    
	@Override
    protected Result runIt(ScriptFactory factory, Mode mode, final String version, TestScriptFactory scriptFactory)
            throws IOException, ScriptException, MojoExecutionException {
	    scriptFactory.setSourceDir(new File("."));
        scriptFactory.emit();

		final Script script = this.factory.newScript(scriptFactory.getCoreScript());
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

        Result result = new Result();
        result.message = "did not find test summary";
        result.success = false;
        
        FileFilter filter = new FileFilter() {
            
            public boolean accept(File f) {
                return !f.getName().matches(".*-[1-9]\\.[1-9]+\\.[^.]+--1.[89].xml$") && 
                        f.getName().endsWith(".xml");
            }
        };
        

        if (testReportDirectory.exists()){
            for (File outputfile : testReportDirectory.listFiles(filter)) {
                System.out.println(outputfile);
                for (Object lineObj : FileUtils.loadFile(outputfile)) {
                    String line = lineObj.toString();
                    if (line.contains("failures")) {
                        line = line.replaceFirst("^<[^\\s]+\\s", "")
                                .replaceFirst(">\\s*$", "");

                        result.message = line;
                        line = line.replaceAll(" name=\"[^\"]+\"", "")
                                .replaceAll("[a-z]+=\"", "")
                                .replaceAll("\"", "").trim();
                        int last = line.lastIndexOf(" ");
                        line = line.substring(0, last);

                        int[] vector = new int[3];
                        int i = 0;
                        for (String n : line.split("\\s+")) {
                            vector[i++] = Integer.parseInt(n);
                        }
                        result.success = (vector[ResultEnum.FAILURES.ordinal()] == 0)
                                && (vector[ResultEnum.ERRORS.ordinal()] == 0);

                        File dest = new File(outputfile.getAbsolutePath()
                                .replaceFirst(".xml$",
                                        "-" + version + 
                                        (mode.flag != null ? mode.flag : "") + 
                                        ".xml"));
                        outputfile.renameTo(dest);
                        if (summaryReport != null) {
                            FileUtils.copyFile(dest, summaryReport);
                        }
                        return result;
                    }
                }
            }
        }
        return result;
	}
}
