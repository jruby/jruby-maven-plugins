package de.saumya.mojo.rails3;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal initialize
 * @phase initialize
 * @requiresDependencyResolution test
 */
@Deprecated
public class InitializeMojo extends AbstractRailsMojo {

    @Override
    protected void executeRails() throws MojoExecutionException {
    }
}
