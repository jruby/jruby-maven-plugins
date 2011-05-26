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
     * @parameter expression="${jrubyc.ignoreFailue}" default-value="false"
     * <br/>
     * Command line -Djrubyc.ignoreFailure=...
     */
    protected boolean ignoreFailures;

    /**
     * @parameter expression="${jrubyc.generateJava}" default-value="false"
     * <br/>
     * Command line -Djrubyc.generateJava=...
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
     *
     * @parameter expression="${jrubyc.verbose}" default-value="false"
     * <br/>
     * Command line -Djrubyc.verbose=...
     */
    private boolean jrubycVerbose;

    //TODO not working for me
//    /**
//     * generate handles for the compiled classes (only with > jruby-1.6.x)
//     *
//     * @parameter expression="${jrubyc.handles}" default-value="false"
//     * <br/>
//     * Command line -Djrubyc.handles=...
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