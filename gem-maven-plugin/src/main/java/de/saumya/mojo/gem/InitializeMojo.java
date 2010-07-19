package de.saumya.mojo.gem;

import java.util.Arrays;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal initialize
 * @phase initialize
 */
public class InitializeMojo extends AbstractGemMojo {

    @Override
    public void execute() throws MojoExecutionException {
        updateMetadata();
        // TODO honor offline mode
        setupGems(Arrays.asList(new Artifact[] { this.project.getArtifact() }));
    }

    @Override
    protected void executeWithGems() throws MojoExecutionException {
        // TODO obsolete
    }
}
