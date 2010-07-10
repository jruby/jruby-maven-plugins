package de.saumya.mojo.jruby;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * maven wrpper around IRB.
 *
 * @goal irb
 * @requiresDependencyResolution test
 */
public class IRBMojo extends AbstractJRubyMojo {

    // override super mojo and make this readonly
    /**
     * @parameter expression="false"
     * @readonly
     */
    protected boolean fork;

    /**
     * arguments for the irb command.
     *
     * @parameter default-value="${jruby.irb.args}"
     */
    protected String  args = null;

    public void execute() throws MojoExecutionException {
        // make sure the whole things run in the same process
        super.fork = false;
        final StringBuilder args = new StringBuilder("-e ENV['GEM_HOME']='"
                + this.gemHome + "';ENV['GEM_PATH']='" + this.gemPath
                + "';$LOAD_PATH<<'./lib' -S irb");
        if (this.args != null) {
            args.append(" ").append(this.args);
        }
        execute(args.toString());
    }
}
