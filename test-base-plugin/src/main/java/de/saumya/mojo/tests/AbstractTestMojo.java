package de.saumya.mojo.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.gem.AbstractGemMojo;
import de.saumya.mojo.jruby.JRubyVersion;
import de.saumya.mojo.jruby.JRubyVersion.Mode;
import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.ruby.script.ScriptFactory;
import de.saumya.mojo.tests.JRubyRun.Result;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * maven wrapper around some test command.
 */
public abstract class AbstractTestMojo extends AbstractGemMojo {

    /**
     */
    @Parameter( defaultValue = "${project.build.directory}/surefire-reports")
    protected File testReportDirectory;

    /**
     * skip all tests
     */
    @Parameter( property = "skipTests", defaultValue = "false")
    protected boolean skipTests;

    /**
     * skip all tests
     */
    @Parameter(property = "maven.test.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * run tests with list of ruby modes 1.8, 1.9, 2.0, 2.2
     */
    @Parameter(property = "jruby.modes")
    protected String modes;

    /**
     * run tests with a several versions of jruby
     */
    @Parameter( property = "jruby.versions")
    protected String versions;

    /**
     * The name of the summary (xml-)report which can be used by TeamCity and Co.
     */
    @Parameter
    protected File summaryReport;

    private Mode[] calculateModes( Mode defaultMode )
    {
        List<Mode> result = new ArrayList<Mode>();
        if (jrubySwitches != null )
        {
            for ( Mode m: Mode.values() )
            {
                if ( jrubySwitches.contains( m.flag ) && ! m.flag.equals( "" ) )
                {
                    result.add( m );
                }
            }
        }
        if (this.modes != null )
        {
            String[] modes = this.modes.split( "[\\ ,;]+" );
            for( String m : modes )
            {
                Mode mode = Mode.valueOf( "_" + m.replace( ".", "" ) );
                if ( ! result.contains( mode ) )
                {
                    result.add( mode );
                }
            }
        }
        if ( result.size() == 0)
        {
            result.add( defaultMode );
        }
        return result.toArray( new Mode[ result.size() ] );
    }
    
    private JRubyVersion[] calculateVersions()
    {
        if ( versions == null )
        {
            return new JRubyVersion[] { getJrubyVersion() };
        }
        else
        {
            String[] jrubyVersions = versions.split("[\\ ,;]+");
            JRubyVersion[] result = new JRubyVersion[ jrubyVersions.length ];
            int i = 0;
            for( String version: jrubyVersions ){
                result[ i++ ] = new JRubyVersion( version );
            }
            return result;
        }
    }
    
    protected void executeWithGems() throws MojoExecutionException, IOException, ScriptException, GemException {
        
        testReportDirectory = new File(testReportDirectory.getAbsolutePath().replace("${project.basedir}/",""));
        List<JRubyRun> runs = new ArrayList<JRubyRun>();
        
        JRubyVersion[] versions = calculateVersions();
        Mode[] modes = calculateModes( getJrubyVersion().defaultMode() );

        if ( versions.length == 1 && versions[ 0 ].equals( getJrubyVersion() ) &&
             modes.length == 1 && modes[0] == getJrubyVersion().defaultMode() )
        {
            runs.add( new JRubyRun( getJrubyVersion() ) );
        }
        else
        {
            for( JRubyVersion version : versions )
            {
                runs.add( new JRubyRun( version, modes ) );
            }
        }

        final File outputDir = new File(this.project.getBuild().getDirectory()
                .replace("${project.basedir}/", ""));
        TestScriptFactory scriptFactory = null;
        for( JRubyRun run: runs){
            scriptFactory = newTestScriptFactory();
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

            if (run.modes.length == 0)
            {
                getLog().warn( "JRuby version " + run.version + " can not run any of the given modes: " + 
                               ( this.modes == null ? Arrays.toString( modes ) : this.modes ) );
            }
            else
            {
                scriptFactory.emit();
            }
        }

        boolean hasOverview = this.versions != null || modes != null;
        if(hasOverview){
            getLog().info("");
            getLog().info("\tOverall Summary");
            getLog().info("\t===============");
        }
        boolean failure = false;
        for( JRubyRun run: runs){
            for(Mode mode: run.modes){
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
        if (getJrubyVersion().equals(run.version) ){//|| run.isDefaultModeOnly()){
            factory = this.factory;
        }
        else {
            try {
                factory = newScriptFactory(resolveJRubyCompleteArtifact(run.version.toString()));
                // TODO remove this - it should have been already taken care of :(
                if( env != null ){
                    for( Map.Entry<String, String> entry: env.entrySet() ){
                        factory.addEnv( entry.getKey(), entry.getValue() );
                    }
                }
            } catch (DependencyResolutionRequiredException e) {
                throw new MojoExecutionException("could not resolve jruby", e);
            }
        }

        for (Mode mode : run.modes) {
            JRubyVersion version = null;
            getLog().info("");
            if ( !run.isDefaultModeOnly ) {
                factory.addSwitch(mode.flag);
                getLog().info("\trun with jruby " + run.version + " in mode " + mode);
                version = run.version;
            }
            else {
                getLog().info("\trun with jruby " + run.version);
                mode = null;
                version = null;
            }
            getLog().info("");
            run.setResult(mode, runIt(factory, mode, version, testScriptFactory));
        }
    }

    protected abstract TestScriptFactory newTestScriptFactory();//Mode mode);
    
    protected abstract Result runIt(ScriptFactory factory, Mode mode, JRubyVersion version, TestScriptFactory testScriptFactory)
        throws IOException, ScriptException, MojoExecutionException;
}
