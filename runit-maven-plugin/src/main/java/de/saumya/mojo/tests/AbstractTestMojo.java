package de.saumya.mojo.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.gem.AbstractGemMojo;
import de.saumya.mojo.jruby.JRubyVersion;
import de.saumya.mojo.jruby.JRubyVersion.Mode;
import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.ruby.script.ScriptFactory;
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
     * @deprecated
     *
     * @parameter expression="${jruby.18and19}"
     */
    protected Boolean use18and19;


    /**
     * run tests with list of ruby modes 1.8, 1.9, 2.0 
     * <br/>
     * Command line -Djruby.modes=1.9,2.0
     *
     * @parameter expression="${jruby.modes}"
     */
    protected String modes;

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

    private Mode[] calculateModes( Mode defaultMode )
    {
        List<Mode> result = new ArrayList<Mode>();
        if( use18and19 != null && use18and19 == true )
        {
            getLog().warn( "use18and19 is deprecated - use modes instead" );
            result.add( Mode._18 );
            result.add( Mode._19 );
        }
        else if (jrubySwitches != null )
        {
            for ( Mode m: Mode.values() )
            {
                if ( jrubySwitches.contains( m.flag ) )
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
                Mode mode = Mode.valueOf( "--" + m );
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
