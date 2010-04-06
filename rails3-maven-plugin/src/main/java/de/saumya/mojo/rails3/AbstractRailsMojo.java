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
    protected String args = null;

    /**
     * @parameter expression="${rails.dir}"
     *            default-value="${project.basedir}/src/main/rails"
     */
    protected File   railsDirectory;

    @Override
    protected File launchDirectory() {
        if (this.railsDirectory.exists()) {
            return this.railsDirectory;
        }
        else {
            return super.launchDirectory();
        }
    }

    protected String binScript(final String script) {
        return new File(binDirectory(), script).getAbsolutePath();
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

    protected String railsScript(final String command) {
        return railsScriptFile().getAbsolutePath() + " " + command;
    }

}
