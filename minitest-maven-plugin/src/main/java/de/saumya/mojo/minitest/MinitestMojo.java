package de.saumya.mojo.minitest;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.ruby.script.ScriptFactory;
import de.saumya.mojo.tests.AbstractTestMojo;
import de.saumya.mojo.tests.JRubyRun.Mode;
import de.saumya.mojo.tests.JRubyRun.Result;
import de.saumya.mojo.tests.TestResultManager;
import de.saumya.mojo.tests.TestScriptFactory;

/**
 * maven wrapper around minitest.
 *
 * @goal test
 * @phase test
 */
public class MinitestMojo extends AbstractTestMojo {

    /**
     * minitest directory with glob to speficy the test files. <br/>
     * Command line -Dminitest.dir=...
     *
     * @parameter expression="${minitest.dir}" default-value="test/**\/*_test.rb"
     */
    private final String minitestDirectory = null;

    /**
     * arguments for the minitest command. <br/>
     * Command line -Drunit.args=...
     *
     * @parameter expression="${minitest.args}"
     */
    private final String minitestArgs = null;

    /**
     * skip the minitests <br/>
     * Command line -DskipMinitests=...
     *
     * @parameter expression="${skipMinitests}" default-value="false"
     */
    protected boolean skipMinitests;
    
    private TestResultManager resultManager;
    private File outputfile;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.skip || this.skipTests || this.skipMinitests) {
            getLog().info("Skipping Minitests");
        } else {
            outputfile = new File(this.project.getBuild().getDirectory()
                    .replace("${project.basedir}/", ""), "minitest.txt");
            if (outputfile.exists()){
                outputfile.delete();
            }
            resultManager = new TestResultManager(project.getName(), "minitest", testReportDirectory, summaryReport);
            super.execute();
        }
    }

    protected Result runIt(ScriptFactory factory, Mode mode, String version, TestScriptFactory scriptFactory)
            throws IOException, ScriptException, MojoExecutionException {
        scriptFactory.setOutputDir(outputfile.getParentFile());
        scriptFactory.setReportPath(outputfile);
        if(minitestDirectory.startsWith(launchDirectory().getAbsolutePath())){
            scriptFactory.setSourceDir(new File(minitestDirectory));
        }
        else{
            scriptFactory.setSourceDir(new File(launchDirectory(), minitestDirectory));
        }

        final Script script = factory.newScript(scriptFactory.getCoreScript());
        if (this.minitestArgs != null) {
            script.addArgs(this.minitestArgs);
        }
        if (this.args != null) {
            script.addArgs(this.args);
        }

        try {
            script.executeIn(launchDirectory());
        } catch (Exception e) {
            getLog().debug("exception in running tests", e);
        }

        return resultManager.generateReports(mode, version, outputfile);
    }

    @Override
    protected TestScriptFactory newTestScriptFactory(Mode mode) {
	// TODO locate minitest gem
        return new MinitestMavenTestScriptFactory(use18and19 == null ? false : use18and19);
    }

}
