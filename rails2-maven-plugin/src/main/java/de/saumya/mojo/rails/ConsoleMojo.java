package de.saumya.mojo.rails;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal to run rails console. it will ignore the fork parameter since forking
 * does not work with a console.
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
    public void executeWithGems() throws MojoExecutionException {
        if (this.version.compareTo("1.5.0") < 0) {
            throw new MojoExecutionException("does not work with jruby version < 1.5.0");
        }
        // make sure the whole things run in the same process
        this.fork = false;
        // no openssl since we are not forking
        this.includeOpenSSL = false;
        final StringBuilder commandArgs = new StringBuilder();
        if (this.arguments != null) {
            for (final String arg : this.arguments.split("\\s+")) {
                commandArgs.append("'").append(arg).append("',");
            }
        }
        if (this.consoleArgs != null) {
            for (final String arg : this.consoleArgs.split("\\s+")) {
                commandArgs.append("'").append(arg).append("',");
            }
        }

        if (this.environment != null) {
            // TODO verify this
            commandArgs.append(" ").append(this.environment);
        }
        execute("-e ENV['GEM_HOME']='" + this.gemHome + "';ENV['GEM_PATH']='"
                + this.gemPath + "';ARGV<<[" + commandArgs
                + "];ARGV.flatten!;load('script/console');", false);
    }
}
