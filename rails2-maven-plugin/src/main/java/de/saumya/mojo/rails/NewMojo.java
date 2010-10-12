package de.saumya.mojo.rails;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.velocity.VelocityContext;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.velocity.VelocityComponent;
import org.sonatype.aether.RepositorySystemSession;

import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.Script;
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
     * 
     * @parameter default-value="${rails.args}"
     */
    protected String                railsArgs                      = null;

    /**
     * the path to the application to be generated
     * 
     * @parameter default-value="${app_path}"
     */
    protected File                  appPath                        = null;

    /**
     * the rails version to use
     * 
     * @parameter default-value="2.3.8" expression="${rails.version}"
     */
    protected String                railsVersion                   = null;

    /**
     * the groupId of the new pom
     * 
     * @parameter default-value="rails" expression="${groupId}"
     */
    protected String                groupId                        = null;

    /**
     * the version of the new pom
     * 
     * @parameter default-value="1.0-SNAPSHOT" expression="${version}"
     */
    protected String                artifactVersion                = null;

    /**
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    private RepositorySystemSession repoSession;

    /** @component */
    private VelocityComponent       velocityComponent;

    // needs to be the default in mojo parameter as well
    private static final String     SMALLEST_ALLOWED_RAILS_VERSION = "2.3.5";

    // ignore rails.dir property if set and execute
    // super.super.lanuchDirectory()
    @Override
    protected File launchDirectory() {
        final File launchDirectory = super.launchDirectory();
        launchDirectory.mkdirs();
        return launchDirectory;
    }

    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException, GemException {
        if (this.railsVersion.compareTo(SMALLEST_ALLOWED_RAILS_VERSION) < 0) {
            getLog().warn("rails version before "
                    + SMALLEST_ALLOWED_RAILS_VERSION + " might not work");
        }
        if (!this.railsVersion.startsWith("2.")) {
            throw new MojoExecutionException("given rails version is not rails2: "
                    + this.railsVersion);
        }
        if (this.project.getBasedir() == null) {

            this.gemsInstaller.installGem("rails",
                                          this.railsVersion,
                                          this.repoSession,
                                          this.localRepository);

        }

        Script script;
        if (railsScriptFile("rails").exists() && this.appPath == null) {
            script = this.factory.newScript(railsScriptFile("rails"));
        }
        else {
            script = this.factory.newScript(this.gemsConfig.binScriptFile("rails"))
                    .addArg("_" + this.railsVersion + "_");

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
            final VelocityContext context = new VelocityContext();

            context.put("groupId", this.groupId);
            context.put("artifactId", this.appPath.getName());
            context.put("version", this.artifactVersion);
            context.put("database", database);
            context.put("railsVersion", this.railsVersion);

            filterContent(this.appPath, context, "pom.xml");

            // write out a new index.html
            filterContent(this.appPath,
                          context,
                          "public/maven.html",
                          "public/index.html");

            // create web.xml
            filterContent(this.appPath,
                          context,
                          "src/main/webapp/WEB-INF/web.xml");

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
