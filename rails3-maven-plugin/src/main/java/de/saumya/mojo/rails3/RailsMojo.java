package de.saumya.mojo.rails3;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;

/**
 * goal to run rails command with the given arguments. either to generate a
 * fresh rails application or to run the rails script from within a rails
 * application.
 * 
 * @goal rails
 * @execute phase="initialize"
 */
public class RailsMojo extends AbstractRailsMojo {

    /**
     * arguments for the rails command
     * 
     * @parameter default-value="${rails.args}"
     */
    protected String railsArgs = null;

    /**
     * the path to the application to be generated
     * 
     * @parameter default-value="${app_path}"
     */
    protected String appPath   = null;

    @Override
    public void executeWithGems() throws MojoExecutionException {
        String commandString;
        if (railsScriptFile().exists() && this.appPath == null) {
            commandString = railsScript("");
        }
        else {
            commandString = binScript("rails");
            if (this.appPath != null) {
                commandString += " " + this.appPath;
            }
        }
        if (this.railsArgs != null) {
            commandString += " " + this.railsArgs;
        }
        if (this.args != null) {
            commandString += " " + this.args;
        }
        execute(commandString, false);
        if (this.appPath != null) {
            final File app = new File(this.project.getBasedir(), this.appPath);
            // rectify the prolog of the script to use ruby instead of jruby
            final File script = new File(new File(app, "script"), "rails");
            try {
                FileUtils.fileWrite(script.getAbsolutePath(),
                                    FileUtils.fileRead(script).replace("jruby",
                                                                       "ruby"));
            }
            catch (final IOException e) {
                throw new MojoExecutionException("failed to filter " + script,
                        e);
            }
            // rectify the Gemfile to allow both ruby + jruby to work
            // TODO make this to work NOT ONLY with sqlite3
            final File gemfile = new File(app, "Gemfile");
            try {
                FileUtils.fileWrite(gemfile.getAbsolutePath(),
                                    FileUtils.fileRead(gemfile)
                                            .replace("\ngem \"sqlite3-ruby\", :require => \"sqlite3\"\n",
                                                     "\ngem \"sqlite3-ruby\", :require => \"sqlite3\" unless defined?(JRUBY_VERSION)\n"
                                                             + "gem \"activerecord-jdbc-adapter\", :require =>'jdbc_adapter' if defined?(JRUBY_VERSION)\n"
                                                             + "gem \"jdbc-sqlite3\", :require => 'jdbc/sqlite3' if defined?(JRUBY_VERSION)\n"));
            }
            catch (final IOException e) {
                throw new MojoExecutionException("failed to filter " + script,
                        e);
            }
            try {
                // TODO obey force, skip and pretend flags !!
                // TODO some screen logging maybe !?
                FileUtils.copyURLToFile(Thread.currentThread()
                        .getContextClassLoader()
                        .getResource("pom.xml"), new File(app, "pom.xml"));
            }
            catch (final IOException e) {
                throw new MojoExecutionException("error copying pom.xml", e);
            }
        }
    }
}
