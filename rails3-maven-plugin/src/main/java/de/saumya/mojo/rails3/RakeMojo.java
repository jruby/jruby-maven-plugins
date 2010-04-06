package de.saumya.mojo.rails3;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * goal to run rails rake with the given arguments.
 * 
 * @goal rake
 * @execute phase="initialize"
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

    //
    // @Override
    // public void execute() throws MojoExecutionException {
    // this.pluginArtifacts.add(this.artifactFactory.createArtifact("rubygems",
    // "rake",
    // "0.8.7",
    // "runtime",
    // "gem"));
    // super.execute();
    // }

    @Override
    public void executeWithGems() throws MojoExecutionException {
        String commandString = binScript("rake");
        if (this.rakeArgs != null) {
            commandString += " " + this.rakeArgs;
        }
        if (this.args != null) {
            commandString += " " + this.args;
        }
        if (this.task != null) {
            commandString += " " + this.task;
        }
        execute(commandString, false);
    }
}
