package de.saumya.mojo.rails3;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * goal to run the rails console. it will ignore the fork parameter since
 * forking does not work with a console.
 * 
 * @goal console
 * @requiresDependencyResolution compile
 */
public class ConsoleMojo extends AbstractRailsMojo {

    /**
     * arguments for the console command
     * 
     * @parameter default-value="${console.args}"
     */
    protected String consoleArgs = null;

    @Override
    protected void executeWithGems() throws MojoExecutionException {
        // make sure the whole things run in the same process
        super.fork = false;
        // no openssl since we are not forking
        this.includeOpenSSL = false;
        final StringBuilder commandArgs = new StringBuilder("'console'");
        if (this.args != null) {
            for (final String arg : this.args.split("\\s+")) {
                commandArgs.append(",'").append(arg).append("'");
            }
        }
        if (this.consoleArgs != null) {
            for (final String arg : this.consoleArgs.split("\\s+")) {
                commandArgs.append(",'").append(arg).append("'");
            }
        }
        if (this.env != null) {
            // TODO verify this
            commandArgs.append(" ").append(this.env);
        }
        execute("-e ENV['GEM_HOME']='" + this.gemHome + "';ENV['GEM_PATH']='"
                + this.gemPath + "';ARGV<<[" + commandArgs
                + "];ARGV.flatten!;load('" + railsScriptFile() + "');", false);
    }
}
