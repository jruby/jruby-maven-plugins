package de.saumya.mojo.rails3;

import java.io.File;

import de.saumya.mojo.gem.AbstractGemMojo;

/**
 * abstract rails mojo which provides a few helper methods and the rails.args
 * parameter.
 */
public abstract class AbstractRailsMojo extends AbstractGemMojo {

    /**
     * arguments for the rails command
     * 
     * @parameter default-value="${args}"
     */
    protected String arguments = null;

    /**
     * @parameter expression="${dir}"
     *            default-value="${project.basedir}/src/main/rails"
     */
    protected File   directory;

    /**
     * either development or test or production or whatever else is possible
     * with your config
     * 
     * @parameter expression="${env}"
     */
    protected String environment;

    @Override
    protected File launchDirectory() {
        if (this.directory.exists()) {
            return this.directory;
        }
        else {
            return super.launchDirectory();
        }
    }

    protected StringBuilder binScript(final String script) {
        return new StringBuilder(new File(binDirectory(), script).getAbsolutePath());
    }

    protected File binDirectory() {
        if (this.gemHome == null) {
            if (System.getenv("GEM_HOME") == null) {
                // TODO something better is needed I guess
                return null;
            }
            else {
                return new File(System.getenv("GEM_HOME"), "bin");
            }
        }
        else {
            return new File(this.gemHome, "bin");
        }
    }

    protected File railsScriptFile() {
        return new File(new File(launchDirectory(), "script"), "rails");
    }

    protected StringBuilder railsScript(final String command) {
        final StringBuilder builder = new StringBuilder(railsScriptFile().getAbsolutePath());
        builder.append(" ").append(command);
        return builder;
    }
}
