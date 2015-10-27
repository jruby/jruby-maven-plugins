package de.saumya.mojo.jruby9.jar;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.FileUtils;

import de.saumya.mojo.jruby9.ArchiveType;
import de.saumya.mojo.jruby9.AbstractGenerateMojo;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * add the gems and jars to resources. @see AbstractGenerateMojo
 * 
 * <br/>
 * 
 * also copies the bootstrap script to the resources if set.
 * 
 * @author christian
 */
@Mojo( name = "generate", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresProject = true,
       threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME )
public class GenerateMojo extends AbstractGenerateMojo {

    @Parameter( defaultValue = "runnable", property = "jruby.archive.type", required = true )
    private ArchiveType type;

    @Parameter( required = false )
    private Boolean pluginDependenciesOnly;

    /**
     * if set this file will be copied as 'jar-bootstrap.rb' to the resources.
     */
    @Parameter( property = "jruby.jar.bootstrap" )
    protected File bootstrap;

    @Override
    protected void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException {
        final boolean pluginDependenciesOnly;
        switch(type) {
        case archive:
            if (this.pluginDependenciesOnly == null) {
                pluginDependenciesOnly = true;
            }
            else {
                pluginDependenciesOnly = this.pluginDependenciesOnly;
            }
            break;
        default:
            pluginDependenciesOnly = false;
        }
        executeWithGems(pluginDependenciesOnly);

        if (bootstrap != null) {
            FileUtils.copyFile(bootstrap, new File(project.getBuild().getOutputDirectory(),
                                                    "jar-bootstrap.rb"));
        }
    }
}
