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
import java.util.HashMap;
import java.util.Map;

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
import org.sonatype.aether.RepositorySystemSession;

import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.gems.GemsInstaller;
import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

@Component(role = RailsManager.class)
public class DefaultRailsManager implements RailsManager {

    public static final String         RAKE_RUBY_COMMAND = "META-INF/jruby.home/bin/rake";
    private static Map<String, String> DATABASES         = new HashMap<String, String>();
    static {
        DATABASES.put("sqlite", "sqilte3");
        DATABASES.put("postgres", "postgresql");
    }

    @Requirement
    private RepositorySystem           repositorySystem;

    @Requirement
    private ProjectBuilder             builder;

    @Requirement
    private VelocityComponent          velocityComponent;

    public void initInstaller(final GemsInstaller installer,
            final File launchDirectory) throws RailsException, IOException {
        patchBootScript(launchDirectory);
        setupWebXML(launchDirectory);
        setupGemfile(installer, launchDirectory);
        setupGemHomeAndGemPath(installer);
    }

    @Deprecated
    private void patchBootScript(final File launchDirectory)
            throws RailsException {
        final File boot = new File(new File(launchDirectory, "config"),
                "boot.rb");
        if (boot.exists() && new File(launchDirectory, "Gemfile.maven").exists()) {
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

    @Deprecated
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

    public void createNew(final GemsInstaller installer,
            final RepositorySystemSession repositorySystemSession,
            final File appPath, String database, String railsVersion,
            final String... args) throws RailsException, GemException,
            IOException, ScriptException

    {
        final File pomFile = new File(appPath, "pom.xml");
        if (pomFile.exists()) {
            throw new RailsException(pomFile
                    + " exists - skip installation of rails");
        }

        // use a rubygems directory in way that the new application can also use
        // it
        if(!installer.config.getGemHome().exists()){
            installer.config.setGemBase(new File(new File(appPath, "target"),
                    "rubygems"));
        }
        setupGemHomeAndGemPath(installer);

        railsVersion = installer.installGem("rails",
                                            railsVersion,
                                            repositorySystemSession,
                                            localRepository()).getVersion();

        // correct spelling
        if (DATABASES.containsKey(database)) {
            database = DATABASES.get(database);
        }

        // run the "rails new"-script
        final Script script = installer.factory.newScript(installer.config.binScriptFile("rails"))
                .addArg("new");
        if (appPath != null) {
            script.addArg(appPath.getAbsolutePath());
        }
        for (final String arg : args) {
            script.addArg(arg);
        }
        if (database != null) {
            script.addArg("-d", database);
        }
        script.execute();

        // correct the database for the jdbc adapter
        database = database.replace("postgresql", "postgres");
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

            installer.factory.newScriptFromResource("maven/tools/pom_generator.rb")
                .addArg("rails")
                .addArg("Gemfile")
                .executeIn(appPath, new File(appPath, "pom.xml"));

            // write out a new index.html
            filterContent(appPath, context, "src/main/webapp/index.html");
            filterContent(appPath,
                          context,
                          "src/main/webapp/index.html",
                          "public/index.html",
                          true);

            setupWebXML(appPath);
        }
    }

    private void setupWebXML(final File launchDirectory) throws RailsException,
            IOException {
        // patch the system only when you find a config/application.rb file
        if (!new File(new File(launchDirectory, "config"), "application.rb").exists()) {
            // TODO log this !!
            return;
        }
        final VelocityContext context = new VelocityContext();
        context.put("basedir", launchDirectory.getAbsolutePath());

        // create web.xml
        filterContent(launchDirectory,
                      context,
                      "src/main/webapp/WEB-INF/web.xml");

        // create override-xyz-web.xml
        filterContent(launchDirectory,
                      context,
                      "target/jetty/override-development-web.xml");
        filterContent(launchDirectory,
                      context,
                      "target/jetty/override-production-web.xml");

        // create the keystore for SSL
        final String keystore = "src/test/resources/server.keystore";
        final File keystoreFile = new File(launchDirectory, keystore);
        if (!keystoreFile.exists()) {
            FileUtils.copyURLToFile(getClass().getResource("/archetype-resources/"
                                            + keystore),
                                    new File(launchDirectory, keystore));
        }

    }

    private void filterContent(final File app, final VelocityContext context,
            final String template) throws RailsException {
        filterContent(app, context, template, template, false);
    }

    private void filterContent(final File app, final VelocityContext context,
            final String template, final String targetName, final boolean force)
            throws RailsException {
        final File templateFile = new File(app, targetName);
        if (!force && templateFile.exists()) {
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

    public void rake(final GemsInstaller installer,
            final RepositorySystemSession repositorySystemSession,
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

    public void generate(final GemsInstaller installer,
            final RepositorySystemSession repositorySystemSession,
            final File launchDirectory, final String generator,
            final String... args) throws IOException, ScriptException,
            GemException, RailsException {
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
            final RepositorySystemSession repositorySystemSession)
            throws IOException, ScriptException, GemException, RailsException {
        final ArtifactRepository localRepository = localRepository();

        final ProjectBuildingRequest pomRequest = new DefaultProjectBuildingRequest().setLocalRepository(localRepository)
                .setRepositorySession(repositorySystemSession)
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

        gemsInstaller.installPom(pom);
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
