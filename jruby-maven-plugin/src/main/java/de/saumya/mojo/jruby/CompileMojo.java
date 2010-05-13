package de.saumya.mojo.jruby;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

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
     * @parameter default-value="src/main/ruby"
     */
    protected File    rubyDirectory;

    /**
     * where the compiled class files are written unless you choose to generate
     * java classes (needs >=jruby-1.5). default is the same as for java
     * classes.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     */
    protected File    outputDirectory;

    /**
     * @parameter expression="${jruby.failure.ignore}" default-value="false"
     */
    protected boolean ignoreFailures;

    /**
     * @parameter expression="${jruby.generate.java}" default-value="false"
     */
    protected boolean generateJava;

    /**
     * where the java files (needs >=jruby-1.5).
     *
     * @parameter default-value="${basedir}/target/jrubyc-generated-sources"
     */
    protected File    generatedJavaDirectory;

    public void execute() throws MojoExecutionException {
        final StringBuilder script = new StringBuilder();
        if (this.generateJava) {
            if (!this.generatedJavaDirectory.exists()) {
                this.generatedJavaDirectory.mkdirs();
            }
            this.project.addCompileSourceRoot(this.generatedJavaDirectory.getAbsolutePath());
        }
        else if (!this.outputDirectory.exists()) {
            this.outputDirectory.mkdirs();
        }
        script.append("-e ")
                .append("require('jruby/jrubyc');status=JRubyCompiler::compile_argv(['-d','")
                .append(this.rubyDirectory.getAbsolutePath());
        if (this.generateJava) {
            script.append("','--java','-t','")
                    .append(this.generatedJavaDirectory.getAbsolutePath());
        }
        else {
            script.append("','-t','")
                    .append(this.outputDirectory.getAbsolutePath());
        }
        script.append("','")
                .append(this.rubyDirectory.getAbsolutePath())
                .append("']);raise('compilation-error(s)')if(status!=0&&!")
                .append(this.ignoreFailures)
                .append(")");
        getLog().debug("script: " + script);
        execute(script.toString());
    }
}
