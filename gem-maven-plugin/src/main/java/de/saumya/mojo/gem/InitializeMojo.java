package de.saumya.mojo.gem;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * installs all declared gem in GEM_HOME
 */
@Mojo( name ="initialize", defaultPhase = LifecyclePhase.INITIALIZE,
       requiresDependencyResolution = ResolutionScope.TEST )
public class InitializeMojo extends AbstractGemMojo {

    @Override
    protected void executeWithGems() throws MojoExecutionException {
    }
}
