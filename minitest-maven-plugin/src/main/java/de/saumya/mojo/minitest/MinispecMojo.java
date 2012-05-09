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
 * maven wrapper around minispec.
 *
 * @goal spec
 * @phase test
 */
public class MinispecMojo extends AbstractTestMojo {

    /**
     * minispec directory with glob to speficy the test files. <br/>
     * Command line -Dminispec.dir=...
     *
     * @parameter expression="${minispec.dir}" default-value="spec/**\/*_spec.rb"
     */
    private final String minispecDirectory = null;

    /**
     * arguments for the minitest command. <br/>
     * Command line -Dminispec.args=...
     *
     * @parameter expression="${minispec.args}"
     */
    private final String minispecArgs = null;

    /**
     * skip the minispecs <br/>
     * Command line -DskipMinispecs=...
     *
     * @parameter expression="${skipMinispecs}" default-value="false"
     */
    protected boolean skipMinispecs;

    private TestResultManager resultManager;
    private File outputfile;

    //TODO
    private boolean useBundler = true;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.skip || this.skipTests || this.skipMinispecs) {
            getLog().info("Skipping Minispecs");
            return;
        } else {
            outputfile = new File(this.project.getBuild().getDirectory()
                    .replace("${project.basedir}/", ""), "minispec.txt");
            resultManager = new TestResultManager(project.getName(), "minispec", testReportDirectory, summaryReport);
            super.execute();
        }
    }

    protected Result runIt(ScriptFactory factory, Mode mode, String version, TestScriptFactory scriptFactory)
            throws IOException, ScriptException, MojoExecutionException {
        
        scriptFactory.setOutputDir(outputfile.getParentFile());
        scriptFactory.setReportPath(outputfile);
        if(minispecDirectory.startsWith(launchDirectory().getAbsolutePath())){
            scriptFactory.setSourceDir(new File(minispecDirectory));
        }
        else{
            scriptFactory.setSourceDir(new File(launchDirectory(), minispecDirectory));
        }

        final Script script = factory.newScript(scriptFactory.getCoreScript());
        if (this.minispecArgs != null) {
            script.addArgs(this.minispecArgs);
        }
        if (this.args != null) {
            script.addArgs(this.args);
        }

        try {
            script.executeIn(launchDirectory());
        } catch (Exception e) {
            getLog().debug("exception in running specs", e);
        }

        return resultManager.generateReports(mode, version, outputfile);
    }

    @Override
    protected TestScriptFactory newTestScriptFactory(Mode mode) {
        // TODO locate minitest gem
        return new MinitestMavenTestScriptFactory(use18and19 == null ? true : use18and19, useBundler);
    }

}
