package de.saumya.mojo.rails3;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * goal to run a generator
 *
 * @goal generate
 * @requiresDependencyResolution test
 */
public class GenerateMojo extends AbstractRailsMojo {

    /**
     * arguments for the generate command
     *
     * @parameter default-value="${generate.args}"
     */
    protected String generateArgs = null;

    /**
     * the name of the generator
     *
     * @parameter default-value="${generator}"
     */
    protected String generator    = null;

    @Override
    protected void executeWithGems() throws MojoExecutionException {
        final StringBuilder command = railsScript("generate");
        if (this.generator != null) {
            command.append(" ").append(this.generator);
        }
        if (this.args != null) {
            command.append(" ").append(this.args);
        }
        if (this.generateArgs != null) {
            command.append(" ").append(this.generateArgs);
        }
        execute(command.toString(), false);
    }
}
