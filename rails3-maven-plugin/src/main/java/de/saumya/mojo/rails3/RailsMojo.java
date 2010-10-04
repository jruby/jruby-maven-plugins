package de.saumya.mojo.rails3;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.rails.RailsException;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * goal to run rails command with the given arguments. either to generate a
 * fresh rails application or to run the rails script from within a rails
 * application.
 * 
 * @goal rails
 */
public class RailsMojo extends AbstractRailsMojo {

    /**
     * arguments for the rails command
     * 
     * @parameter default-value="${rails.args}"
     */
    protected String            railsArgs                      = null;

    /**
     * the path to the application to be generated
     * 
     * @parameter default-value="${app_path}"
     */
    protected File              appPath                        = null;

    /**
     * the rails version to use
     * 
     * @parameter default-value="3.0.0" expression="${railsVersion}"
     */
    // TODO use latest version as default like gemify-plugin
    protected String            railsVersion                   = null;

    /**
     * the groupId of the new pom
     * 
     * @parameter default-value="rails" expression="${groupId}"
     */
    protected String            groupId                        = null;

    /**
     * the version of the new pom
     * 
     * @parameter default-value="1.0-SNAPSHOT" expression="${version}"
     */
    protected String            artifactVersion                = null;

    // needs to be the default in mojo parameter as well
    private static final String SMALLEST_ALLOWED_RAILS_VERSION = "3.0.0.rc";

    // @Override
    // public void executeWithGems() throws MojoExecutionException,
    // ScriptException, IOException, GemException {
    // if (this.railsVersion.length() >= SMALLEST_ALLOWED_RAILS_VERSION.length()
    // && this.railsVersion.compareTo(SMALLEST_ALLOWED_RAILS_VERSION) < 0) {
    // getLog().warn("rails version before "
    // + SMALLEST_ALLOWED_RAILS_VERSION + " might not work");
    // }
    // if (!this.railsVersion.startsWith("3.")) {
    // throw new MojoExecutionException("given rails version is not rails3: "
    // + this.railsVersion);
    // }
    // super.executeWithGems();
    // }

    @Override
    void executeRails() throws MojoExecutionException, ScriptException,
            IOException, GemException, RailsException {
        if (this.railsVersion.length() >= SMALLEST_ALLOWED_RAILS_VERSION.length()
                && this.railsVersion.compareTo(SMALLEST_ALLOWED_RAILS_VERSION) < 0) {
            getLog().warn("rails version before "
                    + SMALLEST_ALLOWED_RAILS_VERSION + " might not work");
        }
        if (!this.railsVersion.startsWith("3.")) {
            throw new MojoExecutionException("given rails version is not rails3: "
                    + this.railsVersion);
        }
        try {
            // this.manager.initInstaller(this.gemsInstaller,
            // launchDirectory());

            final Pattern pattern = Pattern.compile(".*-d\\s+([a-z0-9]+).*");
            final Matcher matcher = pattern.matcher((this.railsArgs == null
                    ? ""
                    : this.railsArgs)
                    + (this.args == null ? "" : this.args));
            final String database;
            if (matcher.matches()) {
                database = matcher.group(1);
            }

            else {
                database = "sqlite3";
            }

            this.manager.createNew(this.gemsInstaller,
                                   this.repoSession,
                                   this.appPath,
                                   database,
                                   this.railsVersion,
                                   joinArgs(this.railsArgs, this.args));
        }
        catch (final RailsException e) {
            throw new MojoExecutionException("error creating new rails application",
                    e);
        }
    }
}
