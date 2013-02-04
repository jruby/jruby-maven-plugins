package de.saumya.mojo.rspec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.tests.AbstractTestMojo;
import de.saumya.mojo.tests.JRubyRun.Mode;
import de.saumya.mojo.tests.JRubyRun.Result;
import de.saumya.mojo.tests.TestScriptFactory;

/**
 * executes the jruby command.
 * 
 * @goal test
 * @phase test
 * @requiresDependencyResolution test
 */
public class RSpecMojo extends AbstractTestMojo {

    /**
     * arguments for the rspec command. <br/>
     * Command line -Drspec.args=...
     *
     * @parameter expression="${rspec.args}"
     */
    private final String rspecArgs = null;


    /**
     * The directory containing the RSpec source files<br/>
     * Command line -Drspec.dir=...
     *
     * @parameter expression="${rspec.dir}" default-value="spec"
     */
    protected String                specSourceDirectory;

    /**
     * skip rspecs <br/>
     * Command line -DskipSpecs=...
     *
     * @parameter expression="${skipSpecs}" default-value="false"
     */
    protected boolean skipSpecs;

    /**
     * The name of the RSpec report.
     * 
     * @parameter default-value="rspec-report.html"
     */
    private String                reportName;

    private File outputfile;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.skip || this.skipTests || this.skipSpecs) {
            getLog().info("Skipping RSpec tests");
            return;
        }
        else {
            outputfile = new File(this.project.getBuild().getDirectory()
                                  .replace("${project.basedir}/", ""), reportName);   
            if (outputfile.exists()){
                outputfile.delete();
            }
            super.execute();
        }
    }
    
    protected Result runIt(de.saumya.mojo.ruby.script.ScriptFactory factory, Mode mode, String version, TestScriptFactory scriptFactory)
            throws IOException, ScriptException, MojoExecutionException {
        
        scriptFactory.setOutputDir(outputfile.getParentFile());
        scriptFactory.setReportPath(outputfile);
        if(specSourceDirectory.startsWith(launchDirectory().getAbsolutePath())){
            scriptFactory.setSourceDir(new File(specSourceDirectory));
        }
        else{
            scriptFactory.setSourceDir(new File(launchDirectory(), specSourceDirectory));
        }

        final Script script = factory.newScript(scriptFactory.getCoreScript());
        if (this.rspecArgs != null) {
            script.addArgs(this.rspecArgs);
        }
        if (this.args != null) {
            script.addArgs(this.args);
        }

        try {
            script.executeIn(launchDirectory());
        } catch (Exception e) {
            getLog().debug("exception in running specs", e);
        }

        String reportPath = outputfile.getAbsolutePath();
      final File reportFile;
      if (mode != Mode.DEFAULT) {
          reportFile = new File(reportPath.replace(".html", "-" + version
                  + mode.name() + ".html"));
      }
      else if (getJrubyVersion().toString().equals(version)) {
          reportFile = new File(reportPath);
      }
      else {
          reportFile = new File(reportPath.replace(".html", "-" + version
                  + ".html"));
      }
      new File(reportPath).renameTo(reportFile);

      Result result = new Result();
      Reader in = null;
      try {
          in = new FileReader(reportFile);
          final BufferedReader reader = new BufferedReader(in);

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
          if (in != null) {
              try {
                  in.close();
              }
              catch (final IOException e) {
                  throw new MojoExecutionException(e.getMessage());
              }
          }
      }

      if (result.message == null) {
        if(reportFile.length() == 0){
            result.success = true;
        }
        else { // this means the report file partial and thus an error occured
            result.message = "An unknown error occurred";
            result.success = false;
        }
      }
      else {
	  String filename = "TEST-rspec"
	      + (mode.flag == null ? "" : "-" + version
		 + mode.flag) + ".xml";
	  File xmlReport = new File(this.testReportDirectory, filename);
	  new File(this.testReportDirectory, "TEST-rspec.xml").renameTo(xmlReport);
	  if (this.summaryReport != null) {
	      FileUtils.copyFile(xmlReport, this.summaryReport);
	  }
	  result.success = result.message.contains("0 failures");
      }
      return result;
    }

    @Override
    protected TestScriptFactory newTestScriptFactory(Mode mode) {
        return new RSpecMavenTestScriptFactory();
    }
}
