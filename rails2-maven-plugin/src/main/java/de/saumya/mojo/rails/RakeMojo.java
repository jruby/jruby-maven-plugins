package de.saumya.mojo.rails;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * goal to run rails rake with the given arguments.
 * 
 * @goal rake
 * @requiresDependencyResolution compile
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
        execute(Arrays.asList(new Artifact[] { this.project.getArtifact() }));
        final StringBuilder command = new StringBuilder(new File(new File(this.gemHome,
                "bin"),
                "rake").getAbsolutePath());
        if (this.rakeArgs != null) {
            command.append(" ").append(this.rakeArgs);
        }
        if (this.args != null) {
            command.append(" ").append(this.args);
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
