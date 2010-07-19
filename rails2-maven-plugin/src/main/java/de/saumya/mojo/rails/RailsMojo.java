package de.saumya.mojo.rails;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.velocity.VelocityContext;
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
     * @parameter default-value="2.3.8" expression="${railsVersion}"
     */
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

    /**
     * @component
     */
    private VelocityComponent   velocityComponent;
    // needs to be the default in mojo parameter as welld
    private static final String SMALLEST_ALLOWED_RAILS_VERSION = "2.3.5";

    @Override
    public void execute() throws MojoExecutionException {
        if (this.railsVersion.compareTo(SMALLEST_ALLOWED_RAILS_VERSION) < 0) {
            getLog().warn("rails version before "
                    + SMALLEST_ALLOWED_RAILS_VERSION + " might not work");
        }
        Artifact artifact = this.artifactFactory.createArtifact("rubygems",
                                                       "rails",
                                                       this.railsVersion,
                                                       "runtime",
                                                       "gem");
        this.pluginArtifacts.add(artifact);
        final DefaultArtifactRepository gemsRepo = new DefaultArtifactRepository("rubygems-releases",
                "http://gems.saumya.de/releases",
                new DefaultRepositoryLayout());
        this.remoteRepositories.add(gemsRepo);
        super.execute();
    }

    // hmm ignore rails.dir property if set so execute
    // super.super.lanuchDirectory()
    @Override
    protected File launchDirectory() {
        if (this.launchDirectory == null) {
            return new File(System.getProperty("user.dir"));
        }
        else {
            this.launchDirectory.mkdirs();
            return this.launchDirectory;
        }
    }

    protected File railsScriptFile() {
        return new File(new File(launchDirectory(), "script"), "rails");
    }

    @Override
    public void executeWithGems() throws MojoExecutionException {
        StringBuilder command;
        if (railsScriptFile().exists() && this.appPath == null) {
            command = railsScript("");
        }
        else {
            command = binScript("rails _" + this.railsVersion + "_ ");
            if (this.appPath != null) {
                command.append(" ").append(this.appPath);
            }
        }
        if (this.railsArgs != null) {
            command.append(" ").append(this.railsArgs);
        }
        if (this.args != null) {
            command.append(" ").append(this.args);
        }
        execute(command.toString(), false);
        if (this.appPath != null) {
            final File app = this.appPath;// new File(launchDirectory(),
            // this.appPath);
            // final File script = new File(app, "script/rails");
            final String database;
            final Pattern pattern = Pattern.compile(".*-d\\s+([a-z0-9]+).*");
            final Matcher matcher = pattern.matcher(command);
            if (matcher.matches()) {
                database = matcher.group(1);
            }
            else {
                database = "sqlite3";
            }
            final VelocityContext context = new VelocityContext();

            context.put("groupId", this.groupId);
            context.put("artifactId", app.getName());
            context.put("version", this.artifactVersion);
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

            // create Gemfile.maven
            filterContent(app, context, "Gemfile.maven");
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
