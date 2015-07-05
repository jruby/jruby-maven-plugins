package de.saumya.mojo.jruby9;

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.gem.AbstractGemMojo;
import de.saumya.mojo.jruby9.JarDependencies.Filter;
import de.saumya.mojo.ruby.script.ScriptException;

public abstract class AbstractGenerateMojo extends AbstractGemMojo {

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
