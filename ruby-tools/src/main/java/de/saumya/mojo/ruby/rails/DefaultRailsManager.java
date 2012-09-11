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
import org.codehaus.plexus.logging.Logger;
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

    @Requirement
    private Logger                     logger;

    public void initInstaller(final GemsInstaller installer,
            final File launchDirectory) throws RailsException, IOException {
        patchBootScript(launchDirectory);
        setupWebXML(launchDirectory);
        setupGemfile(installer, launchDirectory);
    }

    @Deprecated
    private void patchBootScript(final File launchDirectory)
            throws RailsException {
        final File boot = new File(new File(launchDirectory, "config"),
                "boot.rb");
        if (boot.exists() && new File(launchDirectory, "Gemfile.maven").exists()) {
            this.logger.info( "DEPRECATED: the use of Gemfile.maven deprecated. " +
              "use '$ mvn rails:pom' to generate a pom out of the Gemfile");
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

    public void createNew(final GemsInstaller installer,
            final RepositorySystemSession repositorySystemSession,
            final File appPath, String database, String railsVersion,
            ORM orm, final String... args) throws RailsException, GemException,
            IOException, ScriptException {
        createNew(installer, repositorySystemSession, appPath, database, railsVersion, orm,
                  null, null, args);
    }

    public void createNew(final GemsInstaller installer,
                final RepositorySystemSession repositorySystemSession,
                final File appPath, String database, String railsVersion,
                final ORM orm, final String template, final GwtOptions gwt,
                final String... args)
        throws RailsException, GemException, IOException, ScriptException {
        final File pomFile = new File(appPath, "pom.xml");
        if (pomFile.exists()) {
            throw new RailsException(pomFile
                    + " exists - skip installation of rails");
        }

        // use a rubygems directory in way that the new application can also use it
        if(installer.config.getGemHome() == null || !installer.config.getGemHome().exists()){
            installer.config.setGemBase(new File(new File(appPath, "target"),
                    "rubygems"));
        }

        if (railsVersion == null) {
            this.logger.info("NOTE: use rails version 3.0.9 to creates a nice platform independent Gemfile." +
                             " change the rails version anytime later");
        }
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
                .addArg("_" + railsVersion + "_")
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
        if (repositorySystemSession.isOffline()) {
            this.logger.info("system is offline: using jruby rails templates from jar file - might be outdated");
        }
        if (template != null || (gwt != null && gwt.packageName != null)){
            String tmp = templateFrom(orm, repositorySystemSession.isOffline(), railsVersion);
            if(tmp != null ){
                System.setProperty("maven.rails.basetemplate", tmp);
            }
            if (template != null){
                System.setProperty("maven.rails.extratemplate", template);
            }
            if (gwt != null && gwt.packageName != null){
                System.setProperty("maven.rails.gwt", gwt.packageName);
            }
            script.addArg("-m", templateFromResource("templates"));
        }
        else {
            script.addArg("-m", templateFrom(orm, repositorySystemSession.isOffline(), railsVersion));
        }

        // skip bundler
        script.addArg("--skip-bundle");

        script.execute();

        if (appPath != null) {
            installer.factory.newScriptFromResource("maven/tools/pom_generator.rb")
                .addArg("rails")
                .addArg("Gemfile")
                .executeIn(appPath, new File(appPath, "pom.xml"));

            // write out a new index.html
            final VelocityContext context = new VelocityContext();
            context.put("railsVersion", railsVersion);
            filterContent(appPath, context, "src/main/webapp/index.html");
            filterContent(appPath,
                          context,
                          "src/main/webapp/index.html",
                          "public/index.html",
                          true);

            setupWebXML(appPath);

            if (gwt!= null && gwt.packageName != null){
                installer.installGem("activerecord-jdbc" + database + "-adapter",
                                     null,// use the latest version
                                     repositorySystemSession,
                                     localRepository());
                installer.installGem("resty-generators",
                                     null,// use the latest versiona
                                     repositorySystemSession,
                                     localRepository());
                generate(installer,
                         repositorySystemSession,
                         appPath,
                         "resty:setup",
                         gwt.packageName,
                         railsBooleanOption(gwt.session, "session"),
                         railsBooleanOption(gwt.menu, "menu"));
            }
        }
    }

    private String railsBooleanOption(final boolean option, final String name) {
        return "--" + (option ? "" : "skip-") + name;
    }

    private String templateFrom(final ORM orm, final boolean offline, String railsVersion) {
        switch(orm){
        case activerecord:
            if (railsVersion.matches("3.0.[0-9]")){
                if (offline) {
                    return templateFromResource(orm.name());
                }
                return "http://jruby.org/rails3.rb";
            }
            return null;
        case datamapper:
            if (railsVersion.startsWith("3.0.") || offline) {
                return templateFromResource(orm.name());
            }
            return "http://datamapper.org/templates/rails.rb";
        default:
            throw new RuntimeException( "unknown ORM :" + orm);
        }
    }

    private String templateFromResource(final String name) {
        return getClass().getResource("/rails-templates/" + name + ".rb").toString()
                      .replaceFirst("^jar:", "");
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

        // create web.xml unless it exists neither in maven location nor in rails location
        if ( !new File(launchDirectory, "src/main/webapp/WEB-INF/web.xml").exists() ) {
            filterContent(launchDirectory,
                          context,
                          "config/web.xml");
        }

        // create override-xyz-web.xml
        filterContent(launchDirectory,
                      context,
                      "target/jetty/override-development-web.xml");
        filterContent(launchDirectory,
                      context,
                      "target/jetty/override-production-web.xml");

        // create the keystore for SSL
        copyFile(launchDirectory, "src/test/resources/server.keystore");

        // add a monkey patch for throwables
        copyFile(launchDirectory, "config/initializers/java_throwable_monkey_patch.rb");

    }

    private void copyFile(final File launchDirectory, final String name)
            throws IOException {
        final File file = new File(launchDirectory, name);
        if (!file.exists()) {
            FileUtils.copyURLToFile(getClass().getResource("/rails-resources/"
                                            + name), file);
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
        final InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("rails-resources/"
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
        final Script script = installer.factory.newScriptFromSearchPath("rake");
        script.addArgs(task);
        for (final String arg : args) {
            script.addArg(arg);
        }
        if(environment != null && environment.trim().length() > 0){
            script.addArg("RAILS_ENV=" + environment);
        }
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
