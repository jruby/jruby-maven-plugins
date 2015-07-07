package de.saumya.mojo.gem;

import java.io.IOException;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * installs a set of given gems without resolving any transitive dependencies
 */
@Mojo( name = "generate-resources", defaultPhase = LifecyclePhase.GENERATE_RESOURCES )
public class GenerateResourcesMojo extends AbstractGemMojo {

    @Parameter
    protected List<String> includeRubyResources;

    @Parameter
    protected List<String> excludeRubyResources;

    @Parameter
    protected boolean includeBinStubs = false;

    @Override
    protected void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException, GemException {

        if ( includeRubyResources != null) {
            // add it to the classpath so java classes can find the ruby files
            Resource resource = new Resource();
            resource.setDirectory(project.getBasedir().getAbsolutePath());
            for( String include: includeRubyResources) {
                resource.addInclude(include);
            }
            if (excludeRubyResources != null) {
                for( String exclude: excludeRubyResources) {
                    resource.addExclude(exclude);
                }
            }
            addResource(project.getBuild().getResources(), resource);
        }
        
        if (includeBinStubs) {
            Resource resource = new Resource();
            resource.setDirectory(gemsConfig.getBinDirectory().getAbsolutePath());
            resource.addInclude("*");
            resource.setTargetPath("META-INF/jruby.home/bin");
            addResource(project.getBuild().getResources(), resource);
        }
    }
}
