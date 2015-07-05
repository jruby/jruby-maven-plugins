package de.saumya.mojo.jruby9.jar;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.FileUtils;

import de.saumya.mojo.jruby9.AbstractGenerateMojo;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * TODO
 */
@Mojo( name = "generate", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresProject = true,
       threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME )
public class GenerateMojo extends AbstractGenerateMojo {

    /**
     * TODO
     * <br/>
     * Command line -Djar.bootstrap=...
     */
    @Parameter( property = "jar.bootstrap" )
    protected File bootstrap  = null;

    @Override
    protected void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException {
        super.executeWithGems();
        
        if (bootstrap != null) {
            FileUtils.copyFile(bootstrap, new File(project.getBuild().getOutputDirectory(),
                                                    "jar-bootstrap.rb"));
        }
    }
}
