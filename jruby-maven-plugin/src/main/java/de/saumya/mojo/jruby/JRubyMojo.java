package de.saumya.mojo.jruby;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * executes the jruby command.
 * 
 * @goal jruby
 * @requiresDependencyResolution test
 */
public class JRubyMojo extends AbstractJRubyMojo {

    /**
     * ruby code which gets executed.
     * 
     * @parameter default-value="${jruby.script}"
     */
    protected String script = null;

    /**
     * ruby file which gets executed.
     * 
     * @parameter default-value="${jruby.file}"
     */
    protected File   file   = null;

    /**
     * arguments for the jruby command.
     * 
     * @parameter default-value="${jruby.args}"
     */
    protected String args   = null;

    public void execute() throws MojoExecutionException {
        final List<String> args = new ArrayList<String>();
        if (this.script != null && this.script.length() > 0) {
            args.add("-e");
            args.add(this.script);
        }
        if (this.file != null) {
            args.add(this.file.getAbsolutePath());
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