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

/**
 * maven wrapper around the cucumber command.
 * 
 * @goal test
 * @phase test
 * @requiresDependencyResolution test
 */
public class CucumberMojo extends AbstractTestMojo {

    enum ResultEnum {
        ERRORS, FAILURES, SKIPPED, TEST
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


    private TestResultManager resultManager;
    private File outputfile;
    
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (this.skip || this.skipTests || this.skipCucumber) {
			getLog().info("Skipping Cucumber tests");
		} 
		else {
	        if (this.project.getBasedir() != null && 
	                ((this.cucumberDirectory != null && !this.cucumberDirectory.exists()) ||  
	                (this.cucumberDirectory == null && !new File(this.project.getBasedir(), "features").exists())) &&
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
//
//        Result result = new Result();
//        result.message = "did not find test summary";
//        result.success = false;
//        
//        FileFilter filter = new FileFilter() {
//            
//            public boolean accept(File f) {
//                return !f.getName().matches(".*-[1-9]\\.[1-9]+\\.[^.]+--1.[89].xml$") && 
//                        f.getName().endsWith(".xml");
//            }
//        };
//        
//
//        if (testReportDirectory.exists()){
//            for (File outputfile : testReportDirectory.listFiles(filter)) {
//                for (Object lineObj : FileUtils.loadFile(outputfile)) {
//                    String line = lineObj.toString();    
//                    System.out.println(line);
//
//                    if (line.contains("failures")) {
//                        line = line.replaceFirst("^<[^\\s]+\\s", "")
//                                .replaceFirst(">\\s*$", "");
//
//                        result.message = line;
//                        line = line.replaceAll(" name=\"[^\"]+\"", "")
//                                .replaceAll(" time=\"[^\"]+\"", "")
//                                .replaceAll("[a-z]+=\"", "")
//                                .replaceAll("\"", "").trim();
//                        int last = line.lastIndexOf(" ");
//                        line = line.substring(0, last);
//
//                        int[] vector = new int[4];
//                        int i = 0;
//                        for (String n : line.split("\\s+")) {
//                            System.out.println(n);
//                            vector[i++] = Integer.parseInt(n);
//                        }
//                        result.success = (vector[ResultEnum.FAILURES.ordinal()] == 0)
//                                && (vector[ResultEnum.ERRORS.ordinal()] == 0);
//
//                        File dest = new File(outputfile.getAbsolutePath()
//                                .replaceFirst(".xml$",
//                                        "-" + version + 
//                                        (mode.flag != null ? mode.flag : "") + 
//                                        ".xml"));
//                        outputfile.renameTo(dest);
//                        if (summaryReport != null) {
//                            FileUtils.copyFile(dest, summaryReport);
//                        }
//                        return result;
//                    }
//                }
//            }
//        }
//        return result;
	}
}
