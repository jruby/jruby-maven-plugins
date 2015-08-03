package de.saumya.mojo.minitest;

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

/**
 * maven wrapper around minitest.
 */
@Mojo( name = "test", defaultPhase = LifecyclePhase.TEST)
public class MinitestMojo extends AbstractTestMojo {

    /**
     * minitest directory with glob to speficy the test files.
     */
    @Parameter( property = "minitest.dir", defaultValue = "test/**/*_test.rb" )
    private final String minitestDirectory = null;

    /**
     * arguments for the minitest command.
     */
    @Parameter( property = "minitest.args" )
    private final String minitestArgs = null;

    /**
     * skip the minitests
     */
    @Parameter( property = "skipMinitests", defaultValue = "false" )
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

    protected Result runIt(ScriptFactory factory, Mode mode, JRubyVersion version, TestScriptFactory scriptFactory)
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
    protected TestScriptFactory newTestScriptFactory() {
        // TODO locate minitest gem
        return new MinitestMavenTestScriptFactory();
    }

}
