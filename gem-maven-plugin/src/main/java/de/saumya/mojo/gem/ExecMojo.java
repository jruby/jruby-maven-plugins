package de.saumya.mojo.gem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;

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
    protected String script    = null;

    /**
     * ruby file which gets executed in context of the given gems..
     * 
     * @parameter default-value="${gem.exec.file}"
     */
    protected File   file      = null;

    /**
     * arguments for the ruby script given through file parameter.
     * 
     * @parameter default-value="${gem.exec.args}"
     */
    protected String execArgs  = null;

    /**
     * arguments for the jruby command.
     * 
     * @parameter default-value="${jruby.args}"
     */
    protected String jrubyArgs = null;

    /**
     * shortcut for all arguments.
     * 
     * @parameter default-value="${args}"
     */
    protected String args      = null;

    @Override
    public void execute() throws MojoExecutionException {
        // TODO jruby-complete can tries to install gems
        // file:/jruby-complete-1.5.1.jar!/META-INF/jruby.home/lib/ruby/gems/1.8
        // instead of in $HOME/.gem
        this.includeOpenSSL = this.fork;
        super.execute();
    }

    @Override
    public void executeWithGems() throws MojoExecutionException {
        final List<String> args = new ArrayList<String>();
        if (this.jrubyArgs != null) {
            for (final String arg : this.jrubyArgs.split("\\s+")) {
                args.add(arg);
            }
        }
        if (this.script != null && this.script.length() > 0) {
            args.add("-e");
            args.add(this.script);
        }
        else {
            if (this.file != null) {
                args.add(this.file.getAbsolutePath());
            }
            if (this.execArgs != null) {
                for (final String arg : this.execArgs.split("\\s+")) {
                    args.add(arg);
                }
            }
        }
        if (this.args != null) {
            for (final String arg : this.args.split("\\s+")) {
                args.add(arg);
            }
        }
        if (args.size() > 0) {
            execute(args.toArray(new String[args.size()]));
        }
        else {
            getLog().warn("no arguments given. use -Djruby.args=... or -Djruby.script=... or -Djruby.file=...");
        }
    }
}
