package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * executes a ruby script in context of the gems from pom. the arguments for
 * jruby are build like this:
 * <code>${jruby.args} ${exec.file} ${exec.args} ${args}</code> <br/>
 * to execute an inline script the exec parameters are ignored.
 * 
 * @goal exec
 * @requiresDependencyResolution test
 * @execute phase="process-resources"
 */
public class ExecMojo extends AbstractGemMojo {

    /**
     * ruby code from the pom configuration part which gets executed.
     * 
     * @parameter default-value="${gem.exec.script}"
     */
    protected String script   = null;

    /**
     * ruby file which gets executed in context of the given gems..
     * 
     * @parameter default-value="${gem.exec.file}"
     */
    protected File   file     = null;

    /**
     * arguments for the ruby script given through file parameter.
     * 
     * @parameter default-value="${gem.exec.args}"
     */
    protected String execArgs = null;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // TODO jruby-complete tries to install gems
        // file:/jruby-complete-1.5.1.jar!/META-INF/jruby.home/lib/ruby/gems/1.8
        // instead of in $HOME/.gem
        this.includeOpenSSL = this.jrubyFork;
        super.execute();
    }

    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException {
        Script s;
        if (this.script != null && this.script.length() > 0) {
            s = this.factory.newScript(this.script);
        }
        else if (this.file != null) {
            s = this.factory.newScript(this.file);
        }
        else {
            s = this.factory.newArguments();
        }
        s.addArgs(this.execArgs);
        s.addArgs(this.args);
        if (s.isValid()) {
            s.execute();
        }
        else {
            getLog().warn("no arguments given. use -Dgem.exec.args=... or -Dgem.exec.script=... or -Dgem.exec.file=...");
        }
    }
}
