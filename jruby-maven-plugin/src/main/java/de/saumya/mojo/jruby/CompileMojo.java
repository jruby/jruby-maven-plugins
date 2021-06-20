package de.saumya.mojo.jruby;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * executes the compiles ruby classes to java bytecode (jrubyc).
 * 
 * <br/>
 * 
 * NOTE: this goal uses only a small subset of the features of jrubyc.
 */
@Mojo( name = "compile", defaultPhase = LifecyclePhase.COMPILE, 
       requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true )
public class CompileMojo extends AbstractJRubyMojo {

    /**
     * directory where to find the ruby files
     */
    @Deprecated
    @Parameter
    protected File rubyDirectory;

    /**
     * where the compiled class files are written unless you choose to generate
     * java classes (needs >=jruby-1.5). default is the same as for java
     * classes.
     */
    @Parameter( property = "project.build.outputDirectory", defaultValue = "${project.build.outputDirectory}" )
    protected File outputDirectory;

    /**
     * do not fail the goal
     */
    @Parameter( property = "jrubyc.ignoreFailure", defaultValue = "false" )
    protected boolean ignoreFailures;

    /**
     * just generate java classes and add them to the maven source path
     */
    @Parameter( property = "jrubyc.generateJava", defaultValue = "false" )
    protected boolean generateJava;

    /**
     * where the java files (needs >=jruby-1.5).
     */
    @Parameter( defaultValue = "${basedir}/target/jrubyc-generated-sources" )
    protected File generatedJavaDirectory;

    /**
     * verbose jrubyc related output (only with > jruby-1.6.x)
     */
    @Parameter( property = "jrubyc.verbose", defaultValue = "false" )
    private boolean jrubycVerbose;

    @Override
    public void executeJRuby() throws MojoExecutionException, IOException,
            ScriptException {
        if(rubyDirectory != null){
            getLog().warn("please use rubySourceDirectory instead");
        }

        if (this.generateJava) {
            if (!this.generatedJavaDirectory.exists()) {
                this.generatedJavaDirectory.mkdirs();
            }
            this.project.addCompileSourceRoot(this.generatedJavaDirectory
                    .getAbsolutePath());
        }
        else if (!this.outputDirectory.exists()) {
            this.outputDirectory.mkdirs();
        }

        final Script script = this.factory.newScript(
                "\nrequire 'jruby/jrubyc'\n"
                        + "status = 0\n"
                        + "begin\n"
                        + "  status = JRubyCompiler::compile_argv(ARGV)\n"
                        + "rescue Exception\n"
                        + "  puts \"Failure during compilation of file #{ARGV}:\\n#{$!}\"\n"
                        + "  puts $!.backtrace\n"
                        + "  status = 1\n"
                        + "end\n"
                        + "raise 'compilation-error(s)' if status !=0 && !"
                        + this.ignoreFailures);

        if (this.generateJava) {
            script.addArg("--java")
                .addArg("-t", fixPathSeparator(this.generatedJavaDirectory));
        } else {
            script.addArg("-t", fixPathSeparator(this.outputDirectory));
        }

        if(this.jrubyVerbose || this.jrubycVerbose){
            script.addArg("--verbose");
        }
        // add current directory
        script.addArg(".");

        script.executeIn(this.rubyDirectory == null? this.rubySourceDirectory : this.rubyDirectory);
    }

    private String fixPathSeparator(final File f) {
        // http://jira.codehaus.org/browse/JRUBY-5065
        return f.getPath().replace(System.getProperty("file.separator"), "/");
    }
}
