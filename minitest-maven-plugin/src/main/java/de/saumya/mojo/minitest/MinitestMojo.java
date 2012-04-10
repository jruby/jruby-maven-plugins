package de.saumya.mojo.minitest;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

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
 * maven wrapper around minitest.
 *
 * @goal test
 * @phase test
 */
public class MinitestMojo extends AbstractTestMojo {

    enum ResultEnum {
        TESTS, ASSERTIONS, FAILURES, ERRORS, SKIPS
    }

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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.skipTests || this.skipMinitests) {
            getLog().info("Skipping Minitests");
            return;
        } else {
            super.execute();
        }
    }

    protected Result runIt(ScriptFactory factory, Mode mode, String version, TestScriptFactory scriptFactory)
            throws IOException, ScriptException, MojoExecutionException {
        final File outputfile = new File(this.project.getBuild().getDirectory()
                .replace("${project.basedir}/", ""), "minitest.txt");
        
        scriptFactory.setOutputDir(outputfile.getParentFile());
        scriptFactory.setReportPath(outputfile);
        if(minitestDirectory.startsWith(launchDirectory().getAbsolutePath())){
            scriptFactory.setSourceDir(new File(minitestDirectory));
        }
        else{
            scriptFactory.setSourceDir(new File(launchDirectory(), minitestDirectory));
        }

        scriptFactory.emit();

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

        Result result = new Result();
        String time = null;
        for (Object lineObj : FileUtils.loadFile(outputfile)) {
            String line = lineObj.toString();
            if (line.contains("Finished")) {
                time = line.replaceAll("[a-zA-Z]+\\.?", "").trim();
            }
            if (line.contains("failures")) {
                result.message = line;
                int[] vector = new int[5];
                int i = 0;
                String statusLine = line.replaceAll("[a-z]+,?", "");
                for (String n : statusLine.split("\\s+")) {
                    vector[i++] = Integer.parseInt(n);
                }
                result.success = (vector[ResultEnum.FAILURES.ordinal()] == 0)
                        && (vector[ResultEnum.ERRORS.ordinal()] == 0);

                // TODO should be a real report with testcases
                String surefireXml = MessageFormat
                        .format(
                                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                                        + "<testsuite time=\"{0}\" errors=\"{1}\" tests=\"{2}\" skipped=\"{3}\" failures=\"{4}\" name=\"{5}\">\n"
                                        + "</testsuite>\n", time,
                                vector[ResultEnum.ERRORS.ordinal()],
                                vector[ResultEnum.TESTS.ordinal()],
                                vector[ResultEnum.SKIPS.ordinal()],
                                vector[ResultEnum.FAILURES.ordinal()], project
                                        .getName());

                testReportDirectory.mkdirs();
                String filename = "TEST-minitest"
                        + (mode.flag == null ? "" : "-" + version + mode.flag)
                        + ".xml";
                FileUtils.fileWrite(new File(testReportDirectory, filename)
                        .getAbsolutePath(), "UTF-8", surefireXml);
                if (summaryReport != null) {
                    FileUtils.fileWrite(summaryReport.getAbsolutePath(),
                            "UTF-8", surefireXml);
                }
                return result;
            }
        }
        result.message = "did not find test summary";
        result.success = false;
        return result;
    }

    @Override
    protected TestScriptFactory newTestScriptFactory(Mode mode) {
        return new MinitestMavenTestScriptFactory();
    }

}
