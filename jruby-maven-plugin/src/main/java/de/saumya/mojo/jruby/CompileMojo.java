package de.saumya.mojo.jruby;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * executes the compiles ruby classes to java bytecode (jrubyc).
 * 
 * <br/>
 * 
 * NOTE: this goal uses only a small subset of the features of jrubyc.
 * 
 * @goal compile
 * @phase compile
 * @requiresDependencyResolution compile
 */
public class CompileMojo extends AbstractJRubyMojo {

    /**
     * directory where to find the ruby files
     * 
     * @parameter
     */
    @Deprecated
    protected File rubyDirectory;

    /**
     * where the compiled class files are written unless you choose to generate
     * java classes (needs >=jruby-1.5). default is the same as for java
     * classes.
     * 
     * @parameter expression="${project.build.outputDirectory}"
     */
    protected File outputDirectory;

    /**
     * do not fail the goal
     * <br/>
     * Command line -Djrubyc.ignoreFailure=...
     * @parameter expression="${jrubyc.ignoreFailue}" default-value="false"
     */
    protected boolean ignoreFailures;

    /**
     * just generate java classes and add them to the maven source path
     * <br/>
     * Command line -Djrubyc.generateJava=...
     * @parameter expression="${jrubyc.generateJava}" default-value="false"
     */
    protected boolean generateJava;

    /**
     * where the java files (needs >=jruby-1.5).
     * 
     * @parameter default-value="${basedir}/target/jrubyc-generated-sources"
     */
    protected File generatedJavaDirectory;

    /**
     * verbose jrubyc related output (only with > jruby-1.6.x)
     * <br/>
     * Command line -Djrubyc.verbose=...
     *
     * @parameter expression="${jrubyc.verbose}" default-value="false"
     */
    private boolean jrubycVerbose;

    //TODO not working for me
//    /**
//     * generate handles for the compiled classes (only with > jruby-1.6.x)
//     * <br/>
//     * Command line -Djrubyc.handles=...
//     *
//     * @parameter expression="${jrubyc.handles}" default-value="false"
//     */
//    private boolean jrubycHandles;

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
                        + "status = JRubyCompiler::compile_argv(ARGV)\n"
                        + "raise 'compilation-error(s)' if status !=0 && !"
                        + this.ignoreFailures);

        if (this.generateJava) {
            script.addArg("--java")
                .addArg("-t", fixPathSeparator(this.generatedJavaDirectory));
        } else {
            script.addArg("-t", fixPathSeparator(this.outputDirectory));
        }

        if(jrubyVersion.charAt(2) >= '6' && (this.jrubyVerbose || this.jrubycVerbose)){
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