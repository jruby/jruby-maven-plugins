package de.saumya.mojo.rails3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.velocity.VelocityContext;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.velocity.VelocityComponent;

import de.saumya.mojo.ruby.GemException;
import de.saumya.mojo.ruby.RubyScriptException;
import de.saumya.mojo.ruby.Script;

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

    /** @component */
    private VelocityComponent   velocityComponent;

    // needs to be the default in mojo parameter as well
    private static final String SMALLEST_ALLOWED_RAILS_VERSION = "3.0.0.rc";

    @Override
    public void preExecute() throws MojoExecutionException,
            MojoFailureException, IOException, RubyScriptException,
            GemException {
        if (this.railsVersion.compareTo(SMALLEST_ALLOWED_RAILS_VERSION) < 0) {
            getLog().warn("rails version before "
                    + SMALLEST_ALLOWED_RAILS_VERSION + " might not work");
        }
        if (!this.railsVersion.startsWith("3.")) {
            throw new MojoExecutionException("given rails version is not rails3: "
                    + this.railsVersion);
        }
        if (this.appPath != null) {

            setupGems(this.manager.createGemArtifact("rails", this.railsVersion));

            this.manager.addDefaultGemRepositoryForVersion(this.railsVersion,
                                                           this.project.getRemoteArtifactRepositories());
        }
    }

    // // hmm ignore rails.dir property if set so execute
    // // super.super.lanuchDirectory()
    // @Override
    // protected File launchDirectory() {
    // final File launchDirectory = super.launchDirectory();
    // launchDirectory.mkdirs();
    // return launchDirectory;
    // }

    @Override
    public void executeRails() throws MojoExecutionException,
            RubyScriptException, IOException {
        Script script;
        if (railsScriptFile().exists() && this.appPath == null) {
            script = this.factory.newScript(railsScriptFile());
        }
        else {
            script = this.factory.newScript(this.gemService.binScriptFile("rails"))
                    .addArg("_" + this.railsVersion + "_")
                    .addArg("new");
            if (this.appPath != null) {
                script.addArg(this.appPath.getAbsolutePath());
            }
        }
        if (this.railsArgs != null) {
            script.addArgs(this.railsArgs);
        }
        if (this.args != null) {
            script.addArgs(this.args);
        }

        script.execute();
        if (this.appPath != null) {
            final String database;
            final Pattern pattern = Pattern.compile(".*-d\\s+([a-z0-9]+).*");
            final Matcher matcher = pattern.matcher(script.toString());
            if (matcher.matches()) {
                database = matcher.group(1);
            }

            else {
                database = "sqlite3";
            }

            if ("mysql".equals(database)) {

                final File yaml = new File(new File(this.appPath, "config"),
                        "database.yml");
                try {
                    FileUtils.fileWrite(yaml.getAbsolutePath(),
                                        FileUtils.fileRead(yaml)
                                                .replaceAll("mysql2", "mysql"));
                }
                catch (final IOException e) {
                    throw new MojoExecutionException("failed to filter "
                            + script, e);
                }
            }
            // rectify the Gemfile to allow both ruby + jruby to work
            final File gemfile = new File(this.appPath, "Gemfile");
            try {
                FileUtils.fileWrite(gemfile.getAbsolutePath(),
                                    FileUtils.fileRead(gemfile)
                                            .replaceFirst("\ngem (.[^r][a-z0-9-]+.*)\n",
                                                          "\ngem $1 unless defined?(JRUBY_VERSION)\n"
                                                                  + "gem \"activerecord-jdbc-adapter\" if defined?(JRUBY_VERSION)\n"
                                                                  + "gem \"jdbc-"
                                                                  + database
                                                                  + "\", :require => false if defined?(JRUBY_VERSION)\n"));
            }
            catch (final IOException e) {
                throw new MojoExecutionException("failed to filter " + script,
                        e);
            }

            final VelocityContext context = new VelocityContext();

            context.put("groupId", this.groupId);
            context.put("artifactId", this.appPath.getName());
            context.put("version", this.artifactVersion);
            context.put("database", database);
            context.put("railsVersion", this.railsVersion);

            filterContent(this.appPath, context, "pom.xml");

            // write out a new index.html
            filterContent(this.appPath, context, "src/main/webapp/index.html");
            filterContent(this.appPath,
                          context,
                          "src/main/webapp/index.html",
                          "public/index.html");

            // create web.xml
            filterContent(this.appPath,
                          context,
                          "src/main/webapp/WEB-INF/web.xml");

            // create override-xyz-web.xml
            filterContent(this.appPath,
                          context,
                          "src/main/jetty/override-development-web.xml");
            filterContent(this.appPath,
                          context,
                          "src/main/jetty/override-production-web.xml");

            // create Gemfile.maven
            filterContent(this.appPath, context, "Gemfile.maven");
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
