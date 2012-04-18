package de.saumya.mojo.tests;

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
import de.saumya.mojo.tests.JRubyRun.Mode;
import de.saumya.mojo.tests.JRubyRun.Result;

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
     * Command line -DskipTests=...
     * @parameter expression="${skipTests}" default-value="false"
     */
    protected boolean skipTests;

    /**
     * skip all tests
     * <br/>
     * Command line -Dmaven.test.skip=...
     * @parameter expression="${maven.test.skip}" default-value="false"
     */
    protected boolean skip;

    /**
     * run tests for both ruby 1.8 and 1.9
     * <br/>
     * Command line -Djruby.18and19=...
     *
     * @parameter expression="${jruby.18and19}"
     */
    protected Boolean use18and19;


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
            final Mode mode = use18and19 == null? Mode.DEFAULT: Mode._18_19;
            runs.add(new JRubyRun(mode, this.jrubyVersion));
        }
        else {
            final Mode mode;
            if(use18and19 == null || use18and19 == false){
                if(jrubySwitches != null) { 
                    if (jrubySwitches.contains("--1.9")){
                        mode = Mode._19;
                    }
                    else {
                        mode = Mode._18;
                    }
                }
                else {
                    mode = Mode.DEFAULT;
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

        final File outputDir = new File(this.project.getBuild().getDirectory()
                .replace("${project.basedir}/", ""));
        TestScriptFactory scriptFactory = null;
        for( JRubyRun run: runs){
            scriptFactory = newTestScriptFactory(run.mode);
            scriptFactory.setBaseDir(project.getBasedir());
            
            scriptFactory.setGemHome(gemsConfig.getGemHome());
            scriptFactory.setGemPaths(gemsConfig.getGemPath());
            scriptFactory.setOutputDir(outputDir);
            scriptFactory.setSystemProperties(project.getProperties());
            scriptFactory.setSummaryReport(summaryReport);
            scriptFactory.setReportPath(testReportDirectory);
            try {
                scriptFactory.setClasspathElements(project
                        .getTestClasspathElements());
            }
            catch (DependencyResolutionRequiredException e) {
                throw new MojoExecutionException("error getting classpath", e);
            }
            
            runIt(run, scriptFactory);
        }

        scriptFactory.emit();

        boolean hasOverview = this.versions != null || (use18and19 != null && use18and19);
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
            getLog().info("use '" + scriptFactory.getScriptFile() + 
                    "' for faster command line execution.");
        }
        if(failure){
            throw new MojoExecutionException("There were test failures");
        }
    }

    protected void runIt(JRubyRun run, TestScriptFactory testScriptFactory) throws MojoExecutionException, IOException, ScriptException {
        final de.saumya.mojo.ruby.script.ScriptFactory factory;
        if (this.jrubyVersion.equals(run.version) || run.mode == Mode.DEFAULT){
            factory = this.factory;
        }
        else {
            try {
                factory = newScriptFactory(resolveJRubyCompleteArtifact(run.version));
            } catch (DependencyResolutionRequiredException e) {
                throw new MojoExecutionException("could not resolve jruby", e);
            }
        }

        for (Mode mode : run.asSingleModes()) {
            getLog().info("");
            if (mode != Mode.DEFAULT) {
                factory.addSwitch(mode.flag);
                getLog().info("\trun with jruby " + run.version + " in mode " + mode);
            }
            else {
                getLog().info("\trun with jruby " + run.version);
            }
            getLog().info("");
            run.setResult(mode, runIt(factory, mode, run.version, testScriptFactory));
        }
    }

    protected abstract TestScriptFactory newTestScriptFactory(Mode mode);
    
    protected abstract Result runIt(ScriptFactory factory, Mode mode, String version, TestScriptFactory testScriptFactory)
        throws IOException, ScriptException, MojoExecutionException;
}
