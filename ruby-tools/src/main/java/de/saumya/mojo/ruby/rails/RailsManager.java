package de.saumya.mojo.ruby.rails;

import java.io.File;
import java.io.IOException;

import org.sonatype.aether.RepositorySystemSession;

import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.gems.GemsInstaller;
import de.saumya.mojo.ruby.script.ScriptException;

public interface RailsManager {

    public enum ORM { activerecord, datamapper }

    public abstract void initInstaller(final GemsInstaller installer,
            final File launchDirectory) throws RailsException, IOException;

    public abstract void createNew(final GemsInstaller installer,
            final RepositorySystemSession repositorySystemSession,
            final File appPath, String database, final String railsVersion,
            ORM orm, final String... args) throws RailsException, GemException,
            IOException, ScriptException;
    public abstract void createNew(final GemsInstaller installer,
            final RepositorySystemSession repositorySystemSession,
            final File appPath, String database, String railsVersion,
            final ORM orm, final String template, final GwtOptions gwt,
            final String... args) throws RailsException, GemException, IOException,
            ScriptException;

    public abstract void rake(final GemsInstaller installer,
            final RepositorySystemSession repositorySystemSession,
            final File launchDirectory, final String environment,
            final String task, final String... args) throws IOException,
            ScriptException, GemException, RailsException;

    public abstract void generate(final GemsInstaller installer,
            final RepositorySystemSession repositorySystemSession,
            final File launchDirectory, final String generator,
            final String... args) throws IOException, ScriptException,
            GemException, RailsException;

    public abstract void installGems(final GemsInstaller gemsInstaller,
            final RepositorySystemSession repositorySystemSession)
            throws IOException, ScriptException, GemException, RailsException;

}