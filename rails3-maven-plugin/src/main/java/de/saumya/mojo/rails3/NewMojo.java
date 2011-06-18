package de.saumya.mojo.rails3;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.rails.RailsException;
import de.saumya.mojo.ruby.rails.RailsManager.ORM;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * goal to run rails command with the given arguments. either to generate a
 * fresh rails application or to run the rails script from within a rails
 * application.
 *
 * @goal new
 */
public class NewMojo extends AbstractRailsMojo {

    /**
     * arguments for the rails command
     * <br/>
     * Command line -Drails.args=...
     *
     * @parameter default-value="${rails.args}"
     */
    protected String            railsArgs                      = null;

    /**
     * the path to the application to be generated
     * <br/>
     * Command line -Dapp_path=...
     *
     * @parameter default-value="${app_path}"
     */
    protected File              appPath                        = null;

    /**
     * the database to use. DEFAULT: sqlite3
     * <br/>
     * Command line -Ddatabase=...
     *
     * @parameter default-value="${database}"
     */
    protected String            database                       = null;

    /**
     * the rails version to use
     * <br/>
     * Command line -Drails.version=...
     *
     * @parameter expression="${rails.version}"
     */
    protected String            railsVersion                   = null;

    /**
     * the groupId of the new pom
     * <br/>
     * Command line -DgroupId=...
     *
     * @parameter default-value="rails" expression="${groupId}"
     */
    protected String            groupId                        = null;

    /**
     * the version of the new pom
     * <br/>
     * Command line -Dversion=...
     *
     * @parameter default-value="1.0-SNAPSHOT" expression="${version}"
     */
    protected String            artifactVersion                = null;

//    /**
//     * use latest prerelease version unless a rails version is specified.
//     * <br/>
//     * Command line -Dprerelease=...
//     *
//     * @parameter expression="${prerelease}" default-value="false"
//     */
//    protected boolean usePrerelease;

    /**
     * use latest prerelease version unless a rails version is specified.
     * <br/>
     * Command line -Dorm=activerecord or -Dorm=datamapper
     *
     * @parameter expression="${rails.orm}" default-value="activerecord"
     */
    protected String railsORM;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // make sure the whole things run in the same process
        this.jrubyFork = false;
        super.execute();
    }

    @Override
    void executeRails() throws MojoExecutionException, ScriptException,
            IOException, GemException, RailsException {
        if(railsVersion != null && !this.railsVersion.startsWith("3.")) {
            throw new MojoExecutionException("given rails version is not rails-3.x.y : "
                                             + this.railsVersion);
        }
        try {
            if (this.database == null) {
                final Pattern pattern = Pattern.compile(".*-d\\s+([a-z0-9]+).*");
                final Matcher matcher = pattern.matcher((this.railsArgs == null
                        ? ""
                        : this.railsArgs)
                        + (this.args == null ? "" : this.args));
                if (matcher.matches()) {
                    this.database = matcher.group(1);
                }

                else {
                    this.database = "sqlite3";
                }
            }
            final String[] combArgs = joinArgs(this.railsArgs, this.args);
            if (this.appPath == null) {
                // find appPath
                int index = 0;
                for (final String arg : combArgs) {
                    if (this.appPath == null && !arg.startsWith("-")) {
                        this.appPath = new File(arg);
                        break;
                    }
                    index++;
                }
                // remove found appPath from arg list
                if (index < combArgs.length) {
                    combArgs[index] = null;
                }
            }

            this.railsManager.createNew(this.gemsInstaller,
                                        this.repoSession,
                                        this.appPath,
                                        this.database,
                                        this.railsVersion,
                                        ORM.valueOf(this.railsORM),
                                        combArgs);
        }
        catch (final RailsException e) {
            throw new MojoExecutionException("error creating new rails application",
                    e);
        }
    }
}
