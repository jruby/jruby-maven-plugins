package de.saumya.mojo.jruby9.war;

import java.io.IOException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.jruby9.AbstractGenerateMojo;

/**
 * add the gems and jars to resources. @see AbstractGenerateMojo
 * 
 * @author christian
 *
 */
@Mojo( name = "generate", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresProject = true,
       threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME )
public class GenerateMojo extends AbstractGenerateMojo {
    
    @Override
    protected void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException {
        // war files do pack all dependencies the ruby way
        executeWithGems(false);
    }
}
