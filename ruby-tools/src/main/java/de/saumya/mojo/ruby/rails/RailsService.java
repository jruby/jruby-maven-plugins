/**
 * 
 */
package de.saumya.mojo.ruby.rails;

import java.io.File;
import java.io.IOException;

import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.gems.GemManager;
import de.saumya.mojo.ruby.gems.GemsInstaller;
import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.ruby.script.ScriptFactory;

public class RailsService {
    private final GemsInstaller           installer;
    private final RailsManager            manager;
    private final RailsState              state;
    private Object session;

    public RailsService(final RailsState state,
            final Object repositorySystemSession,
            final ScriptFactory factory, final GemManager gemManager,
            final RailsManager manager) throws RailsException, IOException {
        assert state != null;
        assert this.session != null;
        assert factory != null;
        assert manager != null;

        this.state = state;
        this.session = repositorySystemSession;
        this.installer = new GemsInstaller(state.getRubygemsConfig(),
                factory,
                gemManager);
        this.manager = manager;

        manager.initInstaller(this.installer, state.getLaunchDirectory());
    }

    public void resetState() throws RailsException, IOException {
        this.manager.initInstaller(this.installer,
                                   this.state.getLaunchDirectory());
    }

    public void createNew(final String appPath, final String railsVersion,
            final String... args) throws RailsException, GemException,
            IOException, ScriptException {
        // TODO check there is no pom here to avoid conflicts

        this.manager.createNew(this.installer,
                               this.session,
                               new File(appPath),
                               null,
                               railsVersion,
                               null, args);
    }

    public void rake(final String tasks) throws IOException, ScriptException,
            GemException, RailsException {
        this.manager.rake(this.installer,
                          this.session,
                          this.state.getLaunchDirectory(),
                          this.state.getRubygemsConfig().getEnvironment(),
                          tasks,
                          new String[0]);
    }

    public void generate(final String generator, final String... args)
            throws IOException, ScriptException, GemException, RailsException {
        this.manager.generate(this.installer,
                              this.session,
                              this.state.getLaunchDirectory(),
                              generator,
                              args);
    }
}