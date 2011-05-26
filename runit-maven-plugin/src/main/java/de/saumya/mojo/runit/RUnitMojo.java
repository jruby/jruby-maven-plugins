package de.saumya.mojo.runit;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.ruby.script.ScriptFactory;
import de.saumya.mojo.runit.JRubyRun.Mode;
import de.saumya.mojo.runit.JRubyRun.Result;

/**
 * maven wrapper around the runit/testcase command.
 * 
 * @goal test
 */
public class RUnitMojo extends AbstractTestMojo {

    enum ResultEnum {
        TESTS, ASSERTIONS, FAILURES, ERRORS, SKIPS
    }
    
    /**
     * runit directory with glob to be used for the runit command. <br/>
     * Commmand line -Drunit.dir=...
     * 
     * @parameter expression="${runit.dir}" default-value="test/**\/*_test.rb"
     */
    private final String runitDirectory = null;

    /**
     * arguments for the runit command.
     * 
     * @parameter expression="${runit.args}" <br/>
     *            Commmand line -Drunit.args=...
     */
    private final String runitArgs = null;

    /**
     * @parameter expression="${skipRunit}" default-value="false" <br/>
     *            Commmand line -DskipRunit=...
     */
    protected boolean skipRunit;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.skipTests || this.skipRunit) {
            getLog().info("Skipping RUnit tests");
            return;
        } else {
            super.execute();
        }
    }

    protected Result runIt(ScriptFactory factory, Mode mode, String version)
            throws IOException, ScriptException {
        File outputfile = new File(this.project.getBuild().getDirectory()
                .replace("${project.basedir}/", ""), "runit.txt");
        String teeClass = "class Tee < File\n" 
            + "def write(*args)\n"
            + "super\n" 
            + "STDOUT.write *args\n" 
            + "end\n"
            + "def flush(*args)\n" 
            + "super\n" 
            + "STDOUT.flush *args\n"
            + "end\n" 
            + "end\n";
        String addTestCases = "require 'test/unit'\n" 
            + "Dir['"
            + new File(launchDirectory(), this.runitDirectory)
            + "'].each { |f| require f if File.file? f }\n";
        
        final String scriptString;
        if(mode == Mode._18 || (mode == Mode.DEFAULT && 
                (jrubySwitches == null || !jrubySwitches.contains("--1.9")))) {
            scriptString = teeClass
                + "require 'test/unit/ui/console/testrunner'\n"
                + "class Test::Unit::UI::Console::TestRunner\n"
                + "extend Test::Unit::UI::TestRunnerUtilities\n"
                + "alias :old_initialize :initialize\n"
                + "def initialize(suite, output_level=NORMAL)\n"
                + "old_initialize(suite, output_level, Tee.open('"
                + outputfile.getAbsolutePath() + "', 'w'))\n" 
                + "end\n"
                + "end\n" 
                + addTestCases;
        }
        else {
            scriptString = teeClass 
            + "require 'minitest/autorun'\n"
            + "MiniTest::Unit.output = Tee.open('"
            + outputfile.getAbsolutePath() + "', 'w')\n"
            + addTestCases;
        }
        final Script script = factory.newScript(scriptString);
        if (this.runitArgs != null) {
            script.addArgs(this.runitArgs);
        }
        if (this.args != null) {
            script.addArgs(this.args);
        }

        try {
            script.executeIn(launchDirectory());
        }
        catch(Exception e){
            getLog().debug("exception in running tests", e);
        }
        
        Result result = new Result();
        String time = null;
        for (Object lineObj : FileUtils.loadFile(outputfile)) {
            String line = lineObj.toString();
            if(line.contains("Finished")){
                time = line.replaceAll("[a-zA-Z]+\\.?", "").trim();
            }
            if (line.contains("failures")) {
                result.message = line;
                int[] vector = new int[5];
                int i = 0;
                String statusLine = line.replaceAll("[a-z]+,?", "");
                for(String n: statusLine.split("\\s+")){
                    vector[i++] = Integer.parseInt(n);
                }
                result.success = (vector[ResultEnum.FAILURES.ordinal()] == 0) && 
                    (vector[ResultEnum.ERRORS.ordinal()] == 0);
                
                String surefireXml = MessageFormat.format("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<testsuite time=\"{0}\" errors=\"{1}\" tests=\"{2}\" skipped=\"{3}\" failures=\"{4}\" name=\"{5}\">\n" +
                        		"</testsuite>\n", 
                        		time, 
                        		vector[ResultEnum.ERRORS.ordinal()], 
                                vector[ResultEnum.TESTS.ordinal()], 
                                vector[ResultEnum.SKIPS.ordinal()], 
                                vector[ResultEnum.FAILURES.ordinal()],
                        		project.getName());
                
                testReportDirectory.mkdirs();
                String filename = "TEST-runit" + (mode.flag == null ? "" : "-" + version + mode.flag) + ".xml";
                FileUtils.fileWrite(new File(testReportDirectory, filename).getAbsolutePath(), 
                        "UTF-8", 
                        surefireXml);
                if(summaryReport != null){
                    // TODO should be a real report with testcases
                    FileUtils.fileWrite(summaryReport.getAbsolutePath(), 
                            "UTF-8", 
                            surefireXml);
                }
                return result;
            }
        }
        result.message = "did not find test summary";
        result.success = false;
        return result;
    }

}
