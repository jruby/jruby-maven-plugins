package de.saumya.mojo.gem;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * maven wrpper around IRB.
 * 
 * @goal irb
 * @requiresDependencyResolution test
 */
public class IRBMojo extends AbstractGemMojo {

    // override super mojo and make this readonly
    /**
     * @parameter expression="false"
     * @readonly
     */
    protected boolean fork;

    /**
     * arguments for the irb command.
     * 
     * @parameter default-value="${gem.irb.args}"
     */
    protected String  args = null;

    @Override
    public void execute() throws MojoExecutionException {
        // make sure the whole things run in the same process
        super.fork = false;
        // TODO jruby-complete can tries to install gems
        // file:/jruby-complete-1.5.1.jar!/META-INF/jruby.home/lib/ruby/gems/1.8
        // instead of in $HOME/.gem
        this.includeOpenSSL = false;
        super.execute();
    }

    @Override
    public void executeWithGems() throws MojoExecutionException {
        final StringBuilder args = new StringBuilder("-e ENV['GEM_HOME']='"
                + this.gemHome + "';ENV['GEM_PATH']='" + this.gemPath
                + "';$LOAD_PATH<<'./lib' -S irb");
        if (this.args != null) {
            args.append(" ").append(this.args);
        }
        execute(args.toString());
    }
}
