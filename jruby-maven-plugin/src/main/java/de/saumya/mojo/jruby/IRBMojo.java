package de.saumya.mojo.jruby;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * maven wrpper around IRB.
 * 
 * @goal irb
 */
public class IRBMojo extends AbstractJRubyMojo {

    // override super mojo and make this readonly
    /**
     * @parameter expression="false"
     * @readonly
     */
    protected boolean shouldFork;

    /**
     * arguments for the irb command.
     * 
     * @parameter default-value="${jruby.irb.args}"
     */
    protected String  args = null;

    @Override
    public void execute() throws MojoExecutionException {
        super.shouldFork = this.shouldFork;
        final StringBuilder args = new StringBuilder("-S irb");
        if (this.args != null) {
            args.append(" ").append(this.args);
        }
        execute(args.toString());
    }
}
