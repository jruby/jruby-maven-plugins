package de.saumya.mojo.jruby9.jar;

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import de.saumya.mojo.gem.AbstractGemMojo;
import de.saumya.mojo.jruby9.JarDependencies;
import de.saumya.mojo.jruby9.JarDependencies.Filter;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * TODO
 */
@Mojo( name = "generate", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresProject = true,
       threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME )
public class GenerateMojo extends AbstractGemMojo {

    /**
     * The Maven project.
     */
    //@Parameter( defaultValue = "${project}", readonly = true )
   // private MavenProject project;

    //@Parameter( defaultValue = "${plugin}", readonly = true )
    //protected PluginDescriptor  plugin;

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
        JarDependencies jars = new JarDependencies(project.getBuild().getOutputDirectory(), "Jars.lock");
        jars.addAll(plugin.getArtifacts(), new Filter(){

            @Override
            public boolean addIt(Artifact a) {
                return a.getScope().equals("runtime") &&
                        !project.getArtifactMap().containsKey(a.getGroupId() +":" + a.getArtifactId());
            }
            
        });

        jars.addAll(project.getArtifacts());
        jars.generateJarsLock();
        jars.copyJars();
        
        if (bootstrap != null) {
            FileUtils.copyFile(bootstrap, new File(project.getBuild().getOutputDirectory(),
                                                    "jar-bootstrap.rb"));
        }
        
        File pluginGemHome = gemHome( gemsBasePath(), plugin.getArtifactId() );
        addResource(project.getResources(), createGemsResource(pluginGemHome.getAbsolutePath()));
        addResource(project.getResources(), createGemsResource(gemsBasePath()));
    }

    // TODO pull upstream
    protected Resource createGemsResource(String gemHome) {
        Resource resource = new Resource();
        resource.setDirectory(gemHome);
        resource.addInclude("bin/*");
        resource.addInclude("specifications/*");
        resource.addInclude("gems/**");
        resource.addExclude("gems/*/test/**");
        resource.addExclude("gems/*/tests/**");
        resource.addExclude("gems/*/spec/**");
        resource.addExclude("gems/*/specs/**");
        resource.addExclude("gems/*/features/**");
        return resource;
    }

    // TODO pull upstream
    protected String gemsBasePath() {
        String base = this.gemsConfig.getGemHome() != null ? 
                this.gemsConfig.getGemHome().getAbsolutePath() : 
                    (project.getBuild().getDirectory() + "/rubygems");
        return base;
    }
}
