package de.saumya.mojo.rails3;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.gem.AbstractGemMojo;
import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.rails.MavenConfig;
import de.saumya.mojo.ruby.rails.RailsException;
import de.saumya.mojo.ruby.rails.RailsManager;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * abstract rails mojo which provides a few helper methods and the rails.args
 * parameter.
 */
public abstract class AbstractRailsMojo extends AbstractGemMojo {

    /**
     * @parameter expression="${rails.dir}"
     *            default-value="${project.basedir}/src/main/rails"
     */
    protected File         railsDir;

    /**
     * either development or test or production or whatever else is possible
     * with your config
     * 
     * @parameter expression="${rails.env}"
     */
    protected String       env;

    protected MavenConfig  config;

    /** @component */
    protected RailsManager manager;

    protected String[] joinArgs(final String args1, final String args2) {
        final String args = ((args1 == null ? "" : args1) + " " + (args2 == null
                ? ""
                : args2)).trim();
        if ("".equals(args)) {
            return new String[0];
        }
        else {
            return args.split("\\s+");
        }
    }

    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException, GemException {

        try {
            this.manager.initInstaller(this.gemsInstaller, launchDirectory());
            this.config = new MavenConfig();
            // TODO set the config

            executeRails();
        }
        catch (final RailsException e) {
            throw new MojoExecutionException("error executing rails", e);
        }
    }

    abstract void executeRails() throws MojoExecutionException,
            ScriptException, IOException, GemException, RailsException;

    @Override
    protected File launchDirectory() {
        if (this.railsDir.exists()) {
            return this.railsDir;
        }
        else {
            return super.launchDirectory();
        }
    }

    protected File railsScriptFile() {
        return new File(new File(launchDirectory(), "script"), "rails");
    }
}
