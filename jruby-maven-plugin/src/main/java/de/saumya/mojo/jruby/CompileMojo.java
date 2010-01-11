package de.saumya.mojo.jruby;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * executes the compiles ruby classes to java bytecode (jrubyc).
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
     * where the compiled class files are written. default is the same as for
     * java classes.
     * 
     * @parameter default-value="${project.build.outputDirectory}"
     */
    protected File    outputDirectory;

    /**
     * @parameter expression="${jruby.failure.ignore}" default-value="false"
     */
    protected boolean ignoreFailures;

    @Override
    public void execute() throws MojoExecutionException {
        final StringBuilder script = new StringBuilder();
        if (!this.outputDirectory.exists()) {
            this.outputDirectory.mkdirs();
        }
        script.append("-e ")
                .append("require('jruby/jrubyc');status=JRubyCompiler::compile_argv(['-d','")
                .append(this.rubyDirectory.getAbsolutePath())
                .append("','-t','")
                .append(this.outputDirectory.getAbsolutePath())
                .append("','")
                .append(this.rubyDirectory.getAbsolutePath())
                .append("']);raise('compilation-error(s)')if(status!=0&&!")
                .append(this.ignoreFailures)
                .append(")");
        getLog().debug("script: " + script);
        execute(script.toString());
    }
}