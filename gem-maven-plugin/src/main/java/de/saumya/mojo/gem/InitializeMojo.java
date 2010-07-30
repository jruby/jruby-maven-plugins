package de.saumya.mojo.gem;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal initialize
 * @phase initialize
 * @requiresDependencyResolution test
 */
public class InitializeMojo extends AbstractGemMojo {

    @Override
    protected void executeWithGems() throws MojoExecutionException {
    }
}
