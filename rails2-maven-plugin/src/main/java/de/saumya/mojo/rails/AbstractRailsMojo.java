package de.saumya.mojo.rails;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.gem.AbstractGemMojo;

public abstract class AbstractRailsMojo extends AbstractGemMojo {

    private final String scriptName;

    /**
     * @parameter expression="${args}"
     */
    protected String     args;

    /**
     * @parameter expression="${project.basedir}/src/main/rails"
     */
    protected File       railsDirectory;

    /**
     * @parameter expression="${project.basedir}"
     */
    protected File       basedir;

    public AbstractRailsMojo() {
        this("");
    }

    public AbstractRailsMojo(final String scriptName) {
        this.scriptName = scriptName;
    }

    protected boolean hasPomFile() {
        return this.project.getFile() != null;
    }

    @Override
    public void executeWithGems() throws MojoExecutionException {
        execute(Arrays.asList(new Artifact[] { this.project.getArtifact() }));
        String commandString = this.scriptName;
        if (this.args != null) {
            commandString += " " + this.args;
        }
        execute(commandString);
    }

    protected File railsDirectory() {
        if (this.railsDirectory.exists()) {
            return this.railsDirectory;
        }
        else {
            return this.basedir;
        }
    }

    @Override
    protected File launchDirectory() {
        if (this.railsDirectory.exists()) {
            return this.railsDirectory;
        }
        else {
            return super.launchDirectory();
        }
    }
}