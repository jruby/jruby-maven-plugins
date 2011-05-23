package de.saumya.mojo.rspec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.sonatype.aether.RepositorySystemSession;

import de.saumya.mojo.gem.AbstractGemMojo;
import de.saumya.mojo.rspec.JRubyRun.Mode;
import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * executes the jruby command.
 *
 * @goal test
 * @phase test
 * @requiresDependencyResolution test
 */
public class RSpecMojo extends AbstractGemMojo {

    /**
     * The project base directory
     *
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    protected File basedir;

    /**
     * The classpath elements of the project being tested.
     *
     * @parameter expression="${project.testClasspathElements}"
     * @required
     * @readonly
     */
    protected List<String> classpathElements;

    /**
     * The flag to skip tests (optional, defaults to "false")
     *
     * @parameter expression="${maven.test.skip}"
     */
    protected boolean skipTests;

    /** @parameter default-value="${skipSpecs}" */
    protected boolean skipSpecs = false;

    /**
     * The directory containing the RSpec source files
     *
     * @parameter expression="spec"
     */
    protected String specSourceDirectory;

    /**
     * The directory where the RSpec report will be written to
     *
     * @parameter expression="target"
     * @required
     */
    protected File outputDirectory;

    /**
     * The name of the RSpec report (optional, defaults to "rspec-report.html")
     *
     * @parameter expression="rspec-report.html"
     */
    protected String reportName;

    /**
     * The name of the summary (xml-)report which can be used by TeamCity and Co. default is null.
     *
     * @parameter
     */
    protected File summaryReport;

    /**
     * List of system properties to set for the tests.
     *
     * @parameter
     */
    protected Properties systemProperties;

    /**
     * rspec version used when there is no pom. default is latest version.
     *
     * @parameter default-value="${rspec.version}"
     */
    private String rspecVersion;


    /**
     * @parameter default-value="${jruby.18and19}"
     */
    private boolean switch18and19;


    /**
     * @parameter default-value="${jruby.versions}"
     */
    private String versions;

    /**
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    private RepositorySystemSession repoSession;

    private ScriptFactory rspecScriptFactory;

    private File specSourceDirectory() {
        return new File(launchDirectory(), this.specSourceDirectory);
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.skipTests || this.skipSpecs) {
            getLog().info("Skipping RSpec tests");
            return;
        } else {
            super.execute();
        }
    }

    @Override
    public void executeWithGems() throws MojoExecutionException, ScriptException, IOException, GemException {
        final File specSourceDirectory = specSourceDirectory();
        if (!specSourceDirectory.exists() && this.args == null) {
            getLog().info("Skipping RSpec tests since " + specSourceDirectory + " is missing");
            return;
        }
        getLog().info("Running RSpec tests from " + specSourceDirectory);

        if (this.project.getBasedir() == null) {

            this.rspecVersion = this.gemsInstaller.installGem("rspec", this.rspecVersion, this.repoSession, this.localRepository).getVersion();

        }

        final String reportPath = new File(this.outputDirectory, this.reportName).getAbsolutePath();

        initScriptFactory(getRSpecScriptFactory(), reportPath);

        try {
            this.rspecScriptFactory.emit();
        } catch (final Exception e) {
            getLog().error("error emitting .rb", e);
        }

        List<JRubyRun> runs = new ArrayList<JRubyRun>();
        if (versions == null){
            runs.add(new JRubyRun(Mode.DEFAULT, this.jrubyVersion));
        }
        else {
            String[] jrubyVersions = versions.split("[\\ ,;]");
            for(String version: jrubyVersions){
                JRubyRun run = new JRubyRun(switch18and19 ? Mode._18_19 : Mode._18, version);
                runs.add(run);
            }
        }

        for( JRubyRun run: runs){
            runIt(run, reportPath);
        }

        getLog().info("");
        getLog().info("\tOverall RSpec Summary");
        getLog().info("\t=====================");
        boolean failure = false;
        for( JRubyRun run: runs){
            for(Mode mode: run.asSingleModes()){
                getLog().info("\t" + run.toString(mode));
                failure |= !run.success(mode);
            }
        }
        getLog().info("");

        if(failure){
            throw new MojoExecutionException("There were test failures");
        }
    }

    private void runIt(JRubyRun run, String reportPath) throws ScriptException,
            IOException, MojoExecutionException {

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

        if(run.mode != Mode.DEFAULT){
            reportPath.replace(".html", "_" + run.version
                    + ".html");
        }

        for (Mode mode : run.asSingleModes()) {
            if(mode != Mode.DEFAULT){
                factory.addSwitch(mode.flag);
                getLog().info("");
                getLog().info("\trun spec with jruby " + run.version + " in mode " + mode);
                getLog().info("");
            }

            factory.newScript(this.rspecScriptFactory.getScriptFile())
                    .executeIn(launchDirectory());

            final File reportFile;
            if(mode != Mode.DEFAULT){
                reportFile = new File(reportPath.replace(".html", mode.name()
                    + ".html"));
                new File(reportPath).renameTo(reportFile);
            }
            else {
                reportFile = new File(reportPath);
            }

            Reader in = null;
            try {
                in = new FileReader(reportFile);
                final BufferedReader reader = new BufferedReader(in);

                String line = null;

                while ((line = reader.readLine()) != null) {
                    if (line.contains("failures")) {
                        String replaced = line.replaceFirst("\";</.*>", "").replaceFirst("<.*\"", "");
                        run.message(mode, line.replaceFirst("\";</.*>", "").replaceFirst("<.*\"", ""));
                        break;
                    }
                }
            } catch (final IOException e) {
                throw new MojoExecutionException(
                        "Unable to read test report file: " + reportFile);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (final IOException e) {
                        throw new MojoExecutionException(e.getMessage());
                    }
                }
            }
            run.success(mode, run.message(mode).contains("0 failures"));
        }
    }

    private void initScriptFactory(final ScriptFactory factory, final String reportPath) {
        factory.setBaseDir(this.basedir.getAbsolutePath());
        factory.setSummaryReport(this.summaryReport);
        factory.setOutputDir(this.outputDirectory);
        factory.setReportPath(reportPath);
        factory.setSourceDir(specSourceDirectory().getAbsolutePath());
        factory.setClasspathElements(this.classpathElements);
        factory.setGemHome(this.gemHome);
        factory.setGemPaths(new File[] { this.gemPath, new File(this.gemPath.getParentFile(),  this.gemPath.getName() + "-rspec-maven-plugin")});
        Properties props = this.systemProperties;
        if (props == null) {
            props = new Properties();
        }
        factory.setSystemProperties(props);
    }

    private ScriptFactory scriptFactory4Version(String version){
        if (version.startsWith("1.")) {
            return new RSpec1ScriptFactory();
        } else if (version.startsWith("2.")) {
            return new RSpec2ScriptFactory();
        } else {
         return null;
        }
    }

    private ScriptFactory getRSpecScriptFactory() throws MojoExecutionException {
        if (this.rspecScriptFactory != null) {
            return this.rspecScriptFactory;
        }

        this.rspecScriptFactory = getRSpecScriptFactory(project.getDependencyArtifacts());
        if (this.rspecScriptFactory == null) {
            this.rspecScriptFactory = getRSpecScriptFactory(plugin.getArtifacts());
        }

        // get the sciptfactory when there is no pom
        if (this.rspecScriptFactory == null) {
         this.rspecScriptFactory = scriptFactory4Version(this.rspecVersion);
        }

        if (this.rspecScriptFactory == null) {
            throw new MojoExecutionException("Unable to determine version of RSpec");
        }

        return this.rspecScriptFactory;
    }

    private ScriptFactory getRSpecScriptFactory(Collection<Artifact> dependencyArtifacts) {
        for (Artifact each : dependencyArtifacts ) {
            // allow all scope, since with deps within plugins the scope is less important
            if (each.getGroupId().equals("rubygems") && each.getArtifactId().equals("rspec")) {
             return scriptFactory4Version(each.getVersion());
            }
        }
        return null;
    }

}
