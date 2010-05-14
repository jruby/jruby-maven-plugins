package de.saumya.mojo.rails3;

import org.apache.maven.plugin.MojoExecutionException;

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
    public void executeWithGems() throws MojoExecutionException {
        final StringBuilder command = binScript("rake");
        if (this.rakeArgs != null) {
            command.append(" ").append(this.rakeArgs);
        }
        if (this.arguments != null) {
            command.append(" ").append(this.arguments);
        }
        if (this.task != null) {
            command.append(" ").append(this.task);
        }
        if (this.environment != null) {
            command.append(" RAILS_ENV=").append(this.environment);
        }
        execute(command.toString(), false);
    }
}
