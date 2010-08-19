package de.saumya.mojo.rails3;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal initialize
 * @phase initialize
 * @requiresDependencyResolution test
 */
public class InitializeMojo extends AbstractRailsMojo {

    @Override
    protected void executeWithGems() throws MojoExecutionException {
    }
}
