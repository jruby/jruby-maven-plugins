package de.saumya.mojo.rails;

import java.io.File;

import de.saumya.mojo.gem.AbstractGemMojo;

/**
 * abstract rails mojo which provides a few helper methods and the rails.args
 * parameter.
 */
public abstract class AbstractRailsMojo extends AbstractGemMojo {

    /**
     * @parameter expression="${args}"
     */
    protected String args;

    /**
     * @parameter expression="${rails.dir}"
     *            default-value="${project.basedir}/src/main/rails"
     */
    protected File   railsDir;

    /**
     * either development or test or production or whatever else is possible
     * with your config
     * 
     * @parameter expression="${rails.env}"
     */
    protected String env;

    @Override
    protected File launchDirectory() {
        if (this.railsDir.exists()) {
            return this.railsDir;
        }
        else {
            return super.launchDirectory();
        }
    }

    protected File railsScriptFile(final String command) {
        return new File(new File(launchDirectory(), "script"), command);
    }
}
