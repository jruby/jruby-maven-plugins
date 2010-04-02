package de.saumya.mojo.rails3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.velocity.VelocityContext;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.velocity.VelocityComponent;

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
    protected String          railsArgs    = null;

    /**
     * the path to the application to be generated
     *
     * @parameter default-value="${app_path}"
     */
    protected String          appPath      = null;

    /**
     * the rails version to use
     *
     * @parameter default-value="3.0.0.beta" expression="${railsVersion}"
     */
    protected String          railsVersion = null;

    /**
     * the groupId of the new pom
     *
     * @parameter default-value="rails" expression="${groupId}"
     */
    protected String          groupId      = null;

    /**
     * the version of the new pom
     *
     * @parameter default-value="1.0-SNAPSHOT" expression="${version}"
     */
    protected String          version      = null;

    /**
     * @component
     */
    private VelocityComponent velocityComponent;

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
            final File app = new File(launchDirectory(), this.appPath);
            final File script = new File(app, "script/rails");
            final String database;
            final Pattern pattern = Pattern.compile(".*-d\\s+([a-z0-9]+).*");
            final Matcher matcher = pattern.matcher(commandString);
            if (matcher.matches()) {
                database = matcher.group(1);
            }
            else {
                database = "sqlite3";
            }
            // rectify the Gemfile to allow both ruby + jruby to work
            final File gemfile = new File(app, "Gemfile");
            try {
                FileUtils.fileWrite(gemfile.getAbsolutePath(),
                                    FileUtils.fileRead(gemfile)
                                            .replaceFirst("\ngem (\"[^r][a-z0-9-]+\".*)\n",
                                                          "\ngem $1 unless defined?(JRUBY_VERSION)\n"
                                                                  + "gem \"activerecord-jdbc-adapter\", :require =>'jdbc_adapter' if defined?(JRUBY_VERSION)\n"
                                                                  + "gem \"jdbc-"
                                                                  + database
                                                                  + "\", :require => 'jdbc/"
                                                                  + database
                                                                  + "' if defined?(JRUBY_VERSION)\n"));
            }
            catch (final IOException e) {
                throw new MojoExecutionException("failed to filter " + script,
                        e);
            }
            final VelocityContext context = new VelocityContext();

            context.put("groupId", this.groupId);
            context.put("artifactId", app.getName());
            context.put("version", this.version);
            context.put("database", database);
            context.put("railsVersion", this.railsVersion);

            filterContent(app, context, "pom.xml");

            // write out a new index.html
            filterContent(app,
                          context,
                          "public/maven.html",
                          "public/index.html");

            // create web.xml
            filterContent(app, context, "src/main/webapp/WEB-INF/web.xml");

            // create lib/tasks/jdbc.task
            filterContent(app, context, "lib/tasks/jdbc.rake");
        }
    }

    private void filterContent(final File app, final VelocityContext context,
            final String template) throws MojoExecutionException {
        filterContent(app, context, template, template);
    }

    private void filterContent(final File app, final VelocityContext context,
            final String template, final String targetName)
            throws MojoExecutionException {
        final File templateFile = new File(app, targetName);

        final InputStream input = getClass().getResourceAsStream("/archetype-resources/"
                + template);

        try {
            if (input == null) {
                throw new FileNotFoundException(template);
            }

            final String templateString = IOUtil.toString(input);

            templateFile.getParentFile().mkdirs();

            final FileWriter fw = new FileWriter(templateFile);

            this.velocityComponent.getEngine().evaluate(context,
                                                        fw,
                                                        "velocity",
                                                        templateString);

            fw.flush();

            fw.close();
        }
        catch (final IOException e) {
            throw new MojoExecutionException("failed to filter " + template, e);
        }
    }
}
