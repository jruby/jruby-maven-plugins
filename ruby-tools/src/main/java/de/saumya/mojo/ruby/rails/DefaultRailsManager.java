/**
 * 
 */
package de.saumya.mojo.ruby.rails;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.velocity.VelocityContext;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.velocity.VelocityComponent;

import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.gems.GemManager;
import de.saumya.mojo.ruby.gems.GemsInstaller;
import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

@Component(role = RailsManager.class)
public class DefaultRailsManager implements RailsManager {

    public static final String RAKE_RUBY_COMMAND = "META-INF/jruby.home/bin/rake";

    @Requirement
    private RepositorySystem   repositorySystem;

    @Requirement
    private ProjectBuilder     builder;

    @Requirement
    private GemManager         gemManager;

    @Requirement
    private VelocityComponent  velocityComponent;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.sonatype.maven.rails.commands.RailsManager#initInstaller(de.saumya
     * .mojo.ruby.gems.GemsInstaller, java.io.File)
     */
    public void initInstaller(final GemsInstaller installer,
            final File launchDirectory) throws RailsException {
        patchBootScript(launchDirectory);
        setupWebXML(launchDirectory);
        setupGemfile(installer, launchDirectory);
        setupGemHomeAndGemPath(installer);
    }

    private void patchBootScript(final File launchDirectory)
            throws RailsException {
        final File boot = new File(new File(launchDirectory, "config"),
                "boot.rb");
        if (boot.exists()) {
            InputStream bootIn = null;
            InputStream bootOrig = null;
            InputStream bootPatched = null;
            OutputStream bootOut = null;
            try {
                bootIn = new FileInputStream(boot);
                bootOrig = Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream("boot.rb.orig");
                if (IOUtil.contentEquals(bootIn, bootOrig)) {
                    bootIn.close();
                    bootOut = new FileOutputStream(boot);
                    bootPatched = Thread.currentThread()
                            .getContextClassLoader()
                            .getResourceAsStream("boot.rb");
                    IOUtil.copy(bootPatched, bootOut);
                }
            }
            catch (final IOException e) {
                throw new RailsException("error patching config/boot.rb", e);
            }
            finally {
                IOUtil.close(bootIn);
                IOUtil.close(bootOrig);
                IOUtil.close(bootPatched);
                IOUtil.close(bootOut);
            }
        }
    }

    private void setupGemfile(final GemsInstaller installer,
            final File launchDirectory) {
        final File gemfile = new File(launchDirectory, "Gemfile.maven");
        if (gemfile.exists()) {
            installer.factory.addEnv("BUNDLE_GEMFILE", gemfile);
        }
    }

    private void setupGemHomeAndGemPath(final GemsInstaller installer) {
        if (installer.config.hasGemBase()) {
            installer.factory.addEnv("GEM_HOME", installer.config.getGemHome());
            installer.factory.addEnv("GEM_PATH", installer.config.getGemPath());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.sonatype.maven.rails.commands.RailsManager#createNew(de.saumya.mojo
     * .ruby.gems.GemsInstaller, org.sonatype.maven.rails.commands.JRubyConfig,
     * java.lang.String, java.lang.String, java.lang.String)
     */
    public void createNew(final GemsInstaller installer,
            final MavenConfig config, final File appPath,
            final String database, final String railsVersion,
            final String... args) throws RailsException, GemException,
            IOException, ScriptException

    {
        final File pomFile = new File(appPath, "pom.xml");
        if (pomFile.exists()) {
            throw new RailsException(pomFile
                    + " exists - skip installation of rails");
        }

        // setup the rails artifact and use latest version unless specified
        final ArtifactRepository localRepository = localRepository();
        final List<ArtifactRepository> remoteRepositories = new ArrayList<ArtifactRepository>();
        final Artifact rails;
        if (railsVersion == null) {
            this.gemManager.addDefaultGemRepository(remoteRepositories);
            rails = this.gemManager.createGemArtifactWithLatestVersion("rails",
                                                                       localRepository,
                                                                       remoteRepositories);
        }
        else {
            this.gemManager.addDefaultGemRepositoryForVersion(railsVersion,
                                                              remoteRepositories);
            rails = this.gemManager.createGemArtifact("rails", railsVersion);
        }

        // use a rubygems directory in way that the new application can also use
        // it
        installer.config.setGemBase(new File(new File(appPath, "target"),
                "rubygems"));
        setupGemHomeAndGemPath(installer);

        // build a POM for the rails installer and resolve all gem artifacts
        final ProjectBuildingRequest pomRequest = new DefaultProjectBuildingRequest().setLocalRepository(localRepository)
                .setRemoteRepositories(remoteRepositories)
                .setOffline(config.offline)
                .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_STRICT)
                .setResolveDependencies(true);
        MavenProject pom;
        try {

            pom = this.builder.build(rails, pomRequest).getProject();

        }
        catch (final ProjectBuildingException e) {
            throw new RailsException("error building POM for the rails installer",
                    e);
        }
        this.gemManager.resolve(pom.getArtifact(),
                                localRepository,
                                remoteRepositories);

        // install the gems into rubygems
        installer.installGems(pom);

        // run the "rails new"-script
        final Script script = installer.factory.newScript(installer.config.binScriptFile("rails"))
                .addArg("_" + railsVersion + "_")
                .addArg("new");
        if (appPath != null) {
            script.addArg(appPath.getAbsolutePath());
        }
        if (database != null) {
            script.addArg("-d", database);
        }

        script.execute();

        if (appPath != null) {
            if ("mysql".equals(database)) {

                final File yaml = new File(new File(appPath, "config"),
                        "database.yml");
                try {
                    FileUtils.fileWrite(yaml.getAbsolutePath(),
                                        FileUtils.fileRead(yaml)
                                                .replaceAll("mysql2", "mysql"));
                }
                catch (final IOException e) {
                    throw new RailsException("failed to filter " + script, e);
                }
            }
            // rectify the Gemfile to allow both ruby + jruby to work
            final File gemfile = new File(appPath, "Gemfile");
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
                throw new RailsException("failed to filter " + script, e);
            }

            final VelocityContext context = new VelocityContext();

            context.put("groupId", "rails");
            context.put("artifactId", appPath.getName());
            context.put("version", "1.0-SNAPSHOT");
            context.put("database", database);
            context.put("railsVersion", railsVersion);

            filterContent(appPath, context, "pom.xml");

            // write out a new index.html
            filterContent(appPath, context, "src/main/webapp/index.html");
            filterContent(appPath,
                          context,
                          "src/main/webapp/index.html",
                          "public/index.html");

            setupWebXML(appPath);

            // create Gemfile.maven
            filterContent(appPath, context, "Gemfile.maven");
        }
    }

    private void setupWebXML(final File launchDirectory) throws RailsException {
        final VelocityContext context = new VelocityContext();
        // patch the system only when you find a config/application.rb file
        if (!new File(new File(launchDirectory, "config"), "application.rb").exists()) {
            // TODO log this !!
            return;
        }

        // create web.xml
        filterContent(launchDirectory,
                      context,
                      "src/main/webapp/WEB-INF/web.xml");

        // create override-xyz-web.xml
        filterContent(launchDirectory,
                      context,
                      "src/main/jetty/override-development-web.xml");
        filterContent(launchDirectory,
                      context,
                      "src/main/jetty/override-production-web.xml");
    }

    private void filterContent(final File app, final VelocityContext context,
            final String template) throws RailsException {
        filterContent(app, context, template, template);
    }

    private void filterContent(final File app, final VelocityContext context,
            final String template, final String targetName)
            throws RailsException {
        final File templateFile = new File(app, targetName);
        if (templateFile.exists()) {
            return;
        }
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
            throw new RailsException("failed to filter " + template, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.sonatype.maven.rails.commands.RailsManager#rake(de.saumya.mojo.ruby
     * .gems.GemsInstaller, org.sonatype.maven.rails.commands.JRubyConfig,
     * java.io.File, java.lang.String, java.lang.String)
     */
    public void rake(final GemsInstaller installer, final MavenConfig config,
            final File launchDirectory, final String environment,
            final String task, final String... args) throws IOException,
            ScriptException, GemException, RailsException {
        final Script script = installer.factory.newScriptFromResource(RAKE_RUBY_COMMAND);
        script.addArgs(task);
        for (final String arg : args) {
            script.addArg(arg);
        }
        script.addArg("RAILS_ENV=" + environment);

        // final File gemfile = new File( launchDirectory, "Gemfile.maven" );
        // if ( gemfile.exists() )
        // {
        // script.addArg( "BUNDLE_GEMFILE=" + gemfile.getAbsolutePath() );
        // }
        script.executeIn(launchDirectory);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.sonatype.maven.rails.commands.RailsManager#generate(de.saumya.mojo
     * .ruby.gems.GemsInstaller, org.sonatype.maven.rails.commands.JRubyConfig,
     * java.io.File, java.lang.String, java.lang.String)
     */
    public void generate(final GemsInstaller installer,
            final MavenConfig config, final File launchDirectory,
            final String generator, final String... args) throws IOException,
            ScriptException, GemException, RailsException {
        final Script script = installer.factory.newScript(new File(new File(launchDirectory,
                "script"),
                "rails"))
                .addArg("generate")
                .addArg(generator);
        for (final String arg : args) {
            script.addArg(arg);
        }
        script.executeIn(launchDirectory);
    }

    public void installGems(final GemsInstaller gemsInstaller,
            final MavenConfig config) throws IOException, ScriptException,
            GemException, RailsException {
        final ArtifactRepository localRepository = localRepository();

        final ProjectBuildingRequest pomRequest = new DefaultProjectBuildingRequest().setLocalRepository(localRepository)
                .setOffline(config.offline)
                .setForceUpdate(config.forceUpdates)
                .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_STRICT)
                .setResolveDependencies(true);

        MavenProject pom;
        try {

            pom = this.builder.build(new File("pom.xml"), pomRequest)
                    .getProject();

        }
        catch (final ProjectBuildingException e) {
            throw new RailsException("error building the POM", e);
        }

        gemsInstaller.installGems(pom);
    }

    private ArtifactRepository localRepository() throws RailsException {
        ArtifactRepository localRepository;
        try {

            localRepository = this.repositorySystem.createDefaultLocalRepository();

        }
        catch (final InvalidRepositoryException e) {
            throw new RailsException("error creating local repository", e);
        }
        return localRepository;
    }
}
