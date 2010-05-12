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
     * either development or test or production or whatever else is possible
     * with your config
     * 
     * @parameter expression="${env}"
     */
    protected String     environment;

    /**
     * @parameter expression="${rails.dir}"
     *            default-value="${project.basedir}/src/main/rails"
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

    void addEnvironment(final StringBuilder command) {
    }

    @Override
    public void executeWithGems() throws MojoExecutionException {
        execute(Arrays.asList(new Artifact[] { this.project.getArtifact() }));
        final StringBuilder command = new StringBuilder(this.scriptName);
        if (this.environment != null) {
            addEnvironment(command);
        }
        if (this.args != null) {
            command.append(" ").append(this.args);
        }
        execute(command.toString());
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