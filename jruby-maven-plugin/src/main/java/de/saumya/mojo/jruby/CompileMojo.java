package de.saumya.mojo.jruby;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * executes the compiles ruby classes to java bytecode (jrubyc).
 * 
 * @goal compile
 * @requiresDependencyResolution compile
 */
public class CompileMojo extends AbstractJRubyMojo {

    /**
     * 
     * @parameter default-value="src/main/ruby"
     */
    protected File rubyDirectory;

    /**
     * 
     * @parameter default-value="${project.build.outputDirectory}"
     */
    protected File outputDirectory;

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
                .append("']);");
        getLog().debug("script: " + script);
        execute(script.toString());
    }

}