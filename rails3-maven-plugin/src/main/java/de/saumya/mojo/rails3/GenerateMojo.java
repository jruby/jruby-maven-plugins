package de.saumya.mojo.rails3;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * goal to run a generator
 * 
 * @goal generate
 * @execute phase="initialize"
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
        String commandString = railsScript("generate");
        if (this.generator != null) {
            commandString += " " + this.generator;
        }
        if (this.args != null) {
            commandString += " " + this.args;
        }
        if (this.generateArgs != null) {
            commandString += " " + this.generateArgs;
        }
        execute(commandString, false);
    }
}
