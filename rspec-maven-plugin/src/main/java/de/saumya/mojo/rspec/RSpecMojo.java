package de.saumya.mojo.rspec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.codehaus.plexus.util.FileUtils;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.tests.AbstractTestMojo;
import de.saumya.mojo.tests.JRubyRun.Mode;
import de.saumya.mojo.tests.JRubyRun.Result;
import de.saumya.mojo.tests.TestResultManager;
import de.saumya.mojo.tests.TestScriptFactory;

/**
 * executes the jruby command.
 * 
 * @goal test
 * @phase test
 * @requiresDependencyResolution test
 */
public class RSpecMojo extends AbstractTestMojo {

//    /**
//     * The project base directory
//     * 
//     * @parameter expression="${basedir}"
//     * @required
//     * @readonly
//     */
//    protected File                  basedir;
//
//    /**
//     * The classpath elements of the project being tested.
//     * 
//     * @parameter expression="${project.testClasspathElements}"
//     * @required
//     * @readonly
//     */
//    protected List<String>          classpathElements;
//
//    /** @parameter default-value="${skipSpecs}" */
//    protected boolean               skipSpecs = false;

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

//    /**
//     * The directory where the RSpec report will be written to
//     * 
//     * @parameter expression="target"
//     * @required
//     */
//    protected File                  outputDirectory;
//
    /**
     * The name of the RSpec report.
     * 
     * @parameter default-value="rspec-report.html"
     */
    private String                reportName;

//    /**
//     * List of system properties to set for the tests.
//     * 
//     * @parameter
//     */
//    protected Properties            systemProperties;

//    /**
//     * rspec version used when there is no pom. default is latest version.
//     * 
//     * @parameter default-value="${rspec.version}"
//     */
//    private String                  rspecVersion;

//    /**
//     * @parameter default-value="${repositorySystemSession}"
//     * @readonly
//     */
//    private RepositorySystemSession repoSession;
//
//    private ScriptFactory           rspecScriptFactory;

//    private String                  reportPath;

  //  private TestResultManager resultManager;
    private File outputfile;
    
//    private File specSourceDirectory() {
//        return new File(launchDirectory(), this.specSourceDirectory);
//    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.skipTests || this.skipSpecs) {
            getLog().info("Skipping RSpec tests");
            return;
        }
        else {
            outputfile = new File(this.project.getBuild().getDirectory()
                                  .replace("${project.basedir}/", ""), reportName);
//            resultManager = new TestResultManager(project.getName(), "minispec", testReportDirectory, summaryReport);
//
            super.execute();
        }
    }

//    @Override
//    public void executeWithGemss() throws MojoExecutionException,
//            ScriptException, IOException, GemException {
//        final File specSourceDirectory = specSourceDirectory();
//        if (!specSourceDirectory.exists() && this.args == null) {
//            getLog().info("Skipping RSpec tests since " + specSourceDirectory
//                    + " is missing");
//            return;
//        }
//        getLog().info("Running RSpec tests from " + specSourceDirectory);
//
//        if (this.project.getBasedir() == null) {
//
//            this.rspecVersion = this.gemsInstaller.installGem("rspec",
//                                                              this.rspecVersion,
//                                                              this.repoSession,
//                                                              this.localRepository)
//                    .getVersion();
//
//        }
//
//        this.reportPath = new File(this.outputDirectory, this.reportName).getAbsolutePath();
//
//        initScriptFactory(getRSpecScriptFactory(), this.reportPath);
//
//        try {
//            this.rspecScriptFactory.emit();
//        }
//        catch (final Exception e) {
//            getLog().error("error emitting .rb", e);
//        }
//
//        super.executeWithGems();
//    }
    
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
      else if (this.jrubyVersion.equals(version)) {
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
//    protected Result runIt(de.saumya.mojo.ruby.script.ScriptFactory factory,
//            Mode mode, String version) throws IOException, ScriptException,
//            MojoExecutionException {
//        
//        Script script = factory.newScript(this.rspecScriptFactory.getScriptFile());
//        script.addArgs(this.args);
//        script.executeIn(launchDirectory());
//
//        final File reportFile;
//        if (mode != Mode.DEFAULT) {
//            reportFile = new File(reportPath.replace(".html", "-" + version
//                    + mode.name() + ".html"));
//        }
//        else if (this.jrubyVersion.equals(version)) {
//            reportFile = new File(reportPath);
//        }
//        else {
//            reportFile = new File(reportPath.replace(".html", "-" + version
//                    + ".html"));
//        }
//        new File(reportPath).renameTo(reportFile);
//
//        Result result = new Result();
//        Reader in = null;
//        try {
//            in = new FileReader(reportFile);
//            final BufferedReader reader = new BufferedReader(in);
//
//            String line = null;
//
//            while ((line = reader.readLine()) != null) {
//                // singular case needs to be treated as well
//                if (line.contains("failure") && line.contains("example")) {
//                    result.message = line.replaceFirst("\";</.*>", "")
//                            .replaceFirst("<.*\"", "");
//                    break;
//                }
//            }
//        }
//        catch (final IOException e) {
//            throw new MojoExecutionException("Unable to read test report file: "
//                    + reportFile);
//        }
//        finally {
//            if (in != null) {
//                try {
//                    in.close();
//                }
//                catch (final IOException e) {
//                    throw new MojoExecutionException(e.getMessage());
//                }
//            }
//        }
//
//		if (result.message == null) {
//		    if(reportFile.length() == 0){
//	            result.success = true;
//		    }
//		    else { // this means the report file partial and thus an error occured
//		        result.message = "An unknown error occurred";
//		        result.success = false;
//		    }
//		}
//        else {
//			result.success = result.message.contains("0 failures");
//        }
//        return result;
//    }

    @Override
    protected TestScriptFactory newTestScriptFactory(Mode mode) {
        return new RSpecMavenTestScriptFactory();
    }

//    private void initScriptFactory(final ScriptFactory factory,
//            final String reportPath) {
//        factory.setBaseDir(this.basedir.getAbsolutePath());
//        factory.setSummaryReport(this.summaryReport);
//        factory.setOutputDir(this.outputDirectory);
//        factory.setReportPath(reportPath);
//        factory.setSourceDir(specSourceDirectory().getAbsolutePath());
//        factory.setClasspathElements(this.classpathElements);
//        factory.setGemHome(this.gemHome);
//        factory.setGemPaths(new File[] {
//                this.gemPath,
//                new File(this.gemPath.getParentFile(), this.gemPath.getName()
//                        + "-rspec-maven-plugin") });
//        Properties props = this.systemProperties;
//        if (props == null) {
//            props = new Properties();
//        }
//        factory.setSystemProperties(props);
//    }

//    private ScriptFactory scriptFactory4Versionn(String version) {
//        if (version.startsWith("1.")) {
//            return new RSpec1ScriptFactory();
//        }
//        else if (version.startsWith("2.")) {
//            return new RSpec2ScriptFactory();
//        }
//        else {
//            return null;
//        }
//    }
//
//    private ScriptFactory getRSpecScriptFactory() throws MojoExecutionException {
//        if (this.rspecScriptFactory != null) {
//            return this.rspecScriptFactory;
//        }
//
//        this.rspecScriptFactory = getRSpecScriptFactory(project.getDependencyArtifacts());
//        if (this.rspecScriptFactory == null) {
//            this.rspecScriptFactory = getRSpecScriptFactory(plugin.getArtifacts());
//        }
//
//        // get the script-factory when there is no pom
//        if (this.rspecScriptFactory == null) {
//            if(this.rspecVersion == null && this.project.getBasedir() != null){
//                throw new MojoExecutionException("please add a gem dependency for rspec to your POM");
//            }
//            this.rspecScriptFactory = new RSpec2ScriptFactory();//scriptFactory4Version(this.rspecVersion);
//        }
//
//        if (this.rspecScriptFactory == null) {
//            throw new MojoExecutionException("Unable to determine version of RSpec");
//        }
//
//        return this.rspecScriptFactory;
//    }
//
//    private ScriptFactory getRSpecScriptFactory(
//            Collection<Artifact> dependencyArtifacts) {
//        for (Artifact each : dependencyArtifacts) {
//            // allow all scope, since with deps within plugins the scope is less
//            // important
//            if (each.getGroupId().equals("rubygems")
//                    && each.getArtifactId().equals("rspec")) {
//                return scriptFactory4Version(each.getVersion());
//            }
//        }
//        return null;
//    }

}
