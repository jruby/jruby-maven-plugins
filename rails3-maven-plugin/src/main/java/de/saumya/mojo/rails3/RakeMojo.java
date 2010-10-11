package de.saumya.mojo.rails3;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.rails.RailsException;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * goal to run rails rake with the given arguments.
 * 
 * @goal rake
 * @requiresDependencyResolution test
 */
public class RakeMojo extends AbstractRailsMojo {

    /**
     * arguments for the generate command
     * 
     * @parameter default-value="${rake.args}"
     */
    protected String rakeArgs = null;

    /**
     * the path to the application to be generated
     * 
     * @parameter default-value="${task}"
     */
    protected String task     = null;

    @Override
    public void executeRails() throws MojoExecutionException, ScriptException,
            IOException, GemException, RailsException {
        this.manager.rake(this.gemsInstaller,
                          this.repoSession,
                          launchDirectory(),
                          this.env,
                          this.task == null ? null : this.task.trim(),
                          joinArgs(this.rakeArgs, this.args));
    }
}
