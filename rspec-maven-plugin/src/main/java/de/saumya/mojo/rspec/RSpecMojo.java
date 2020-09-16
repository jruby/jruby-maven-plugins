package de.saumya.mojo.rspec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import de.saumya.mojo.jruby.JRubyVersion;
import de.saumya.mojo.jruby.JRubyVersion.Mode;
import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.tests.AbstractTestMojo;
import de.saumya.mojo.tests.JRubyRun.Result;
import de.saumya.mojo.tests.TestScriptFactory;

/**
 * executes the jruby command.
 */
@Mojo(name ="test", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class RSpecMojo extends AbstractTestMojo {

    /**
     * arguments for the rspec command.
     */
    @Parameter( property = "rspec.args")
    private final String rspecArgs = null;


    /**
     * The directory containing the RSpec source files
     */
    @Parameter(property = "rpsec.dir", defaultValue = "spec")
    protected String                specSourceDirectory;

    /**
     * skip rspecs
     */
    @Parameter(property = "skipSpecs", defaultValue = "false")
    protected boolean skipSpecs;

    /**
     * The name of the RSpec report.
     */
    @Parameter(defaultValue = "rspec-report.html")
    private String                reportName;

    private File outputfile;

    private File specsDir;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.skip || this.skipTests || this.skipSpecs) {
            getLog().info("Skipping RSpec tests");
            return;
        }
        else 
        {
            this.specsDir = specSourceDirectory.startsWith( launchDirectory().getAbsolutePath() ) ?
                            new File( specSourceDirectory ) : 
                            new File( launchDirectory(), specSourceDirectory );
            if ( !this.specsDir.isDirectory() )
            {
                getLog().info("given " + specSourceDirectory + " does not exist - skipping RSpec test'" );
                return;
            }
            outputfile = new File( this.project.getBuild().getDirectory().replace( "${project.basedir}/", ""),
                                   reportName );   
            if (outputfile.exists())
            {
                outputfile.delete();
            }
            super.execute();
        }
    }
    
    protected Result runIt(de.saumya.mojo.ruby.script.ScriptFactory factory, Mode mode, 
                JRubyVersion version, TestScriptFactory scriptFactory)
            throws IOException, ScriptException, MojoExecutionException {
        
        scriptFactory.setOutputDir( outputfile.getParentFile() );
        scriptFactory.setReportPath( outputfile );
        scriptFactory.setSourceDir( specsDir );

        final Script script = factory.newScript(scriptFactory.getCoreScript());
        if (this.rspecArgs != null) {
            script.addArgs(this.rspecArgs);
        }
        if (this.args != null) {
            script.addArgs(this.args);
        }

        Exception error = null;
        try {
            script.executeIn(launchDirectory());
        } catch (Exception e) {
            error = e;
            getLog().debug("exception in running specs", e);
        }

        String reportPath = outputfile.getAbsolutePath();
      final File reportFile;
      if ( version != null && modes != null ) {
          reportFile = new File(reportPath.replace(".html", "-" + version
                  + mode.name() + ".html"));
      }
      else if ( versions == null || versions.isEmpty() || version == null ) {
          reportFile = new File(reportPath);
      }
      else {
          reportFile = new File(reportPath.replace(".html", "-" + version
                  + ".html"));
      }
      new File(reportPath).renameTo(reportFile);

      Result result = new Result();
      BufferedReader reader = null;
      try {
          reader = new BufferedReader(new FileReader(reportFile));

          String line = null;

          while ((line = reader.readLine()) != null) {
              // singular case needs to be treated as well
              if (line.contains("failure") && line.contains("example")) {
                  result.message = line.replaceFirst("\";</.*>", "")
                          .replaceFirst("<.*\"", "");
                  break;
              }
          }
      }
      catch (final IOException e) {
          throw new MojoExecutionException("Unable to read test report file: "
                  + reportFile);
      }
      finally {
          IOUtil.close(reader);
      }

      if (result.message == null) {
        if(reportFile.length() == 0 ){
            if ( error == null ){
                result.success = true;
            }
            else{
                result.message = "An error occurred";
                result.success = false;                
            }                
        }
        else { // this means the report file partial and thus an error occured
            result.message = "An unknown error occurred";
            result.success = false;
        }
      }
      else {
          String filename = "TEST-rspec"
                  + ( mode == null ? "" : ( version == null ? "" : "-" + version + mode.flag ) ) 
                  + ".xml";

          File xmlReport = new File(this.testReportDirectory, filename);
          new File(this.testReportDirectory, "TEST-rspec.xml").renameTo(xmlReport);
          if (this.summaryReport != null) {
              FileUtils.copyFile(xmlReport, this.summaryReport);
          }
          result.success = result.message.contains("0 failures") || result.message.contains("Failing...0");
      }
      return result;
    }

    @Override
    protected TestScriptFactory newTestScriptFactory() {
        return new RSpecMavenTestScriptFactory();
    }
}
