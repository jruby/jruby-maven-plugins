package de.saumya.mojo.rails3;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * goal to run the rails database console
 * 
 * @goal dbconsole
 * @requiresDependencyResolution compile
 */
public class DbConsoleMojo extends AbstractRailsMojo {

    /**
     * arguments for the database console command
     * 
     * @parameter default-value="${dbconsole.args}"
     */
    protected String dbconsoleArgs = null;

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
        if (this.dbconsoleArgs != null) {
            for (final String arg : this.dbconsoleArgs.split("\\s+")) {
                commandArgs.append(",'").append(arg).append("'");
            }
        }
        if (this.env != null) {
            // TODO verify this
            commandArgs.append(this.env);
        }
        executeScript(railsScriptFile(), commandArgs.toString(), false);
    }
}
