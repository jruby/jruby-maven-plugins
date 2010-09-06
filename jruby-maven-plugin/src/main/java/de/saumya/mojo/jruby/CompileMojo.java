package de.saumya.mojo.jruby;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.RubyScriptException;
import de.saumya.mojo.ruby.Script;

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
    protected File rubyDirectory;

    /**
     * where the compiled class files are written unless you choose to generate
     * java classes (needs >=jruby-1.5). default is the same as for java
     * classes.
     * 
     * @parameter default-value="${project.build.outputDirectory}"
     */
    protected File outputDirectory;

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
    protected File generatedJavaDirectory;

    @Override
    public void executeJRuby() throws MojoExecutionException, IOException,
            RubyScriptException {
        if (this.generateJava) {
            if (!this.generatedJavaDirectory.exists()) {
                this.generatedJavaDirectory.mkdirs();
            }
            this.project.addCompileSourceRoot(this.generatedJavaDirectory
                    .getAbsolutePath());
        } else if (!this.outputDirectory.exists()) {
            this.outputDirectory.mkdirs();
        }

        final Script script = this.factory.newScript(
                "\nrequire 'jruby/jrubyc'\n"
                        + "status=JRubyCompiler::compile_argv(ARGV)\n"
                        + "raise 'compilation-error(s)' if status !=0 && !"
                        + this.ignoreFailures).addArg("-d",
                fixPathSeparator(this.rubyDirectory));

        if (this.generateJava) {
            script.addArg("--java").addArg("-t",
                    fixPathSeparator(this.generatedJavaDirectory));
        } else {
            script.addArg("-t", fixPathSeparator(this.outputDirectory));
        }
        script.addArg(this.rubyDirectory);
        script.execute();
    }

    private String fixPathSeparator(final File f) {
        // http://jira.codehaus.org/browse/JRUBY-5065
        return f.getPath().replace(System.getProperty("file.separator"), "/");
    }
}