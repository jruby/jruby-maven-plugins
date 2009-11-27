package de.saumya.mojo.jruby;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * executes the jruby command.
 * 
 * @goal jruby
 * @requiresDependencyResolution test
 */
public class JRubyMojo extends AbstractJRubyMojo {

    /**
     * ruby script which gets executed.
     * 
     * @parameter default-value="${jruby.script}"
     */
    protected String script = null;

    /**
     * arguments for the jruby command.
     * 
     * @parameter default-value="${jruby.args}"
     */
    protected String args   = null;

    @Override
    public void execute() throws MojoExecutionException {
        final StringBuilder args = new StringBuilder();
        if (this.script != null && this.script.length() > 0) {
            args.append("-e ").append(this.script);
        }
        if (this.args != null) {
            args.append(" ").append(this.args);
        }
        if (args.length() > 0) {
            System.out.println("asd: " + args);
            execute(args.toString());
        }
        else {
            getLog().warn("no arguments given. use -Djruby.args=... or -Djruby.script=...");
        }
    }

}