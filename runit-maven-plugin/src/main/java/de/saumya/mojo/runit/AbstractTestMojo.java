package de.saumya.mojo.runit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.gem.AbstractGemMojo;
import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.ruby.script.ScriptFactory;
import de.saumya.mojo.runit.JRubyRun.Mode;
import de.saumya.mojo.runit.JRubyRun.Result;

/**
 * maven wrapper around some test command.
 *
 * @phase test
 * @requiresDependencyResolution test
 */
public abstract class AbstractTestMojo extends AbstractGemMojo {

    /**
     * @parameter expression="${project.build.directory}/surefire-reports"
     */
    protected File testReportDirectory;

    /**
     * skip all tests
     * <br/>
     * Command line -Dmaven.test.skip=...
     * @parameter expression="${maven.test.skip}" default-value="false"
     */
    protected boolean skipTests;

    /**
     * run tests for both ruby 1.8 and 1.9
     * <br/>
     * Command line -Djruby.18and19=...
     *
     * @parameter expression="${jruby.18and19}"
     */
    private Boolean switch18and19;


    /**
     * run tests with a several versions of jruby
     * <br/>
     * Command line -Djruby.versions=...
     *
     * @parameter expression="${jruby.versions}"
     */
    private String versions;

    /**
     * The name of the summary (xml-)report which can be used by TeamCity and Co.
     *
     * @parameter
     */
    protected File summaryReport;

    protected void executeWithGems() throws MojoExecutionException, IOException, ScriptException, GemException {
        testReportDirectory = new File(testReportDirectory.getAbsolutePath().replace("${project.basedir}/",""));
        List<JRubyRun> runs = new ArrayList<JRubyRun>();
        if (versions == null){
            final Mode mode = switch18and19 == null? Mode.DEFAULT: Mode._18_19;
            runs.add(new JRubyRun(mode, this.jrubyVersion));
        }
        else {
            final Mode mode;
            if(switch18and19 == null || switch18and19 == false){
                if(jrubySwitches != null && jrubySwitches.contains("--1.9")){
                    mode = Mode._19;
                }
                else {
                    mode = Mode._18;
                }
            }
            else {
                mode = Mode._18_19;
            }
            String[] jrubyVersions = versions.split("[\\ ,;]");
            for(String version: jrubyVersions){
                JRubyRun run = new JRubyRun(mode, version);
                runs.add(run);
            }
        }

        for( JRubyRun run: runs){
            runIt(run);
        }

        boolean hasOverview = this.versions != null || (switch18and19 != null && switch18and19);
        if(hasOverview){
            getLog().info("");
            getLog().info("\tOverall Summary");
            getLog().info("\t===============");
        }
        boolean failure = false;
        for( JRubyRun run: runs){
            for(Mode mode: run.asSingleModes()){
                if(hasOverview){
                    getLog().info("\t" + run.toString(mode));
                }
                failure |= !run.result(mode).success;
            }
        }
        if(hasOverview){
            getLog().info("");
        }
        if(failure){
            throw new MojoExecutionException("There were test failures");
        }
    }

    protected void runIt(JRubyRun run) throws MojoExecutionException, IOException, ScriptException {
        final de.saumya.mojo.ruby.script.ScriptFactory factory;
        if (this.jrubyVersion.equals(run.version) || run.mode == Mode.DEFAULT){
            factory = this.factory;
        }
        else {
            try {
                factory = newScriptFactory(resolveJRUBYCompleteArtifact(run.version));
            } catch (DependencyResolutionRequiredException e) {
                throw new MojoExecutionException("could not resolve jruby", e);
            }
        }

        for (Mode mode : run.asSingleModes()) {
            if (mode != Mode.DEFAULT) {
                factory.addSwitch(mode.flag);
                getLog().info("");
                getLog().info("\trun with jruby " + run.version + " in mode " + mode);
                getLog().info("");
            }
            run.setResult(mode, runIt(factory, mode, run.version));
        }
    }


    protected abstract Result runIt(ScriptFactory factory, Mode mode, String version)
        throws IOException, ScriptException, MojoExecutionException;
}
