package de.saumya.mojo.gem;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.jruby.AbstractJRubyMojo;

/**
 * goal to run "gem list".
 * 
 * @goal list
 */
public class ListMojo extends AbstractJRubyMojo {
    /**
     * arguments for the gem command of JRuby.
     * 
     * @parameter default-value="${jruby.gem.args}"
     */
    protected String args = null;

    @Override
    public void execute() throws MojoExecutionException {
        String commandString = "-S gem list";
        if (this.args != null) {
            commandString += " " + this.args;
        }
        execute(commandString);
    }
}
