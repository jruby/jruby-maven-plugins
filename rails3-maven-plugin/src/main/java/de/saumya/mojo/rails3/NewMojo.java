package de.saumya.mojo.rails3;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.rails.GwtOptions;
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
@Deprecated
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
     * the database to use
     * <br/>
     * Command line -Ddatabase=...
     *
     * @parameter expression="${database}" default-value="sqlite3"
     */
    protected String            database                       = null;

    /**
     * rails template to apply after create the application
     * <br/>
     * Command line -Dtemplate=...
     *
     * @parameter expression="${template}"
     */
    protected String            template                       = null;

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

    /**
     * select the ORM to use
     * <br/>
     * Command line -Dorm=activerecord or -Dorm=datamapper
     *
     * @parameter expression="${orm}" default-value="activerecord"
     */
    protected String railsORM;

    /**
     * when the gwt package is given then the rails gets GWT as view component
     * <br/>
     * Command line -Dgwt.package=...
     *
     * @parameter expression="${gwt.package}"
     */
    protected String gwtPackage;

    /**
     * setup GWT with session support
     * <br/>
     * Command line -Dgwt.session=true
     *
     * @parameter expression="${gwt.session}" default-value="false"
     */
    protected boolean gwtSession;

    /**
     * setup GWT with menu support
     * <br/>
     * Command line -Dgwt.menu=true
     *
     * @parameter expression="${gwt.menu}" default-value="false"
     */
    protected boolean gwtMenu;



    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // make sure the whole things run in the same process
        this.jrubyFork = false;
        super.execute();
    }

    @Override
    void executeRails() throws MojoExecutionException, ScriptException,
            IOException, GemException, RailsException {
        getLog().warn( "DEPRECATED: just do not use that anymore. use gem:exec or bundler:exec instead" );
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

            getLog().info("use ORM " + ORM.valueOf(this.railsORM));
            GwtOptions gwt = new GwtOptions(gwtPackage, gwtSession, gwtMenu);
            this.railsManager.createNew(this.gemsInstaller,
                                        this.repoSession,
                                        this.appPath,
                                        this.database,
                                        this.railsVersion,
                                        ORM.valueOf(this.railsORM),
                                        this.template,
                                        gwt,
                                        combArgs);
        }
        catch (final RailsException e) {
            throw new MojoExecutionException("error creating new rails application",
                    e);
        }
    }
}
