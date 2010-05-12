package de.saumya.mojo.rails;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal to run rails console.
 * 
 * @goal console
 * @requiresDependencyResolution compile
 */
public class ConsoleMojo extends AbstractRailsMojo {

    // override super mojo and make this readonly
    /**
     * @parameter expression="false"
     * @readonly
     */
    protected boolean fork;

    // override super mojo and make this readonly
    /**
     * @parameter expression="false"
     * @readonly
     */
    protected boolean includeOpenSSLGem;

    /**
     * arguments for the console command
     * 
     * @parameter default-value="${console}"
     */
    protected String  consoleArgs = null;

    public ConsoleMojo() {
        // no openssl since we are not forking
        super.includeOpenSSLGem = false;
    }

    @Override
    void addEnvironment(final StringBuilder scriptName) {
        scriptName.append(" ").append(this.environment);
    }

    @Override
    public void executeWithGems() throws MojoExecutionException {
        if (this.jrubyVersion.compareTo("1.5.0") < 0) {
            throw new MojoExecutionException("does not work with jruby version < 1.5.0");
        }
        // make sure the whole things run in the same process
        super.fork = false;
        final StringBuilder commandArgs = new StringBuilder();
        if (this.args != null) {
            for (final String arg : this.args.split("\\s+")) {
                commandArgs.append("'").append(arg).append("',");
            }
        }
        if (this.consoleArgs != null) {
            for (final String arg : this.consoleArgs.split("\\s+")) {
                commandArgs.append("'").append(arg).append("',");
            }
        }
        execute("-e ENV['GEM_HOME']='" + this.gemHome + "';ENV['GEM_PATH']='"
                + this.gemPath + "';ARGV<<[" + commandArgs
                + "];ARGV.flatten!;load('script/console');", false);
    }
}