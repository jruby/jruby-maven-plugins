package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * installs a set of given gems without resolving any transitive dependencies
 * 
 * @goal sets
 * @phase initialize
 */
public class SetsMojo extends AbstractGemMojo {
    
    private static final String[] SCOPES = new String[] { "provided", "test" };
    
    /**
     * the scope under which the gems get installed
     * 
     * @parameter default-value="compile"
     */
    protected String scope;

    /**
     * map of gemname to version, i.e. it is a "list" of gems with fixed version
     * 
     * @parameter
     */
    protected Map<String, String>  gems = Collections.emptyMap();
    
    @Override
    protected void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException, GemException {
        List<Artifact> artifacts = new LinkedList<Artifact>();
        for( Map.Entry<String, String> gem : gems.entrySet() ) {
            Set<Artifact> set = manager.resolve( manager.createGemArtifact( gem.getKey(),
                                                                            gem.getValue() ),
                                               localRepository,
                                               project.getRemoteArtifactRepositories() );
           if ( set.size() == 1 )
           {
               artifacts.add( set.iterator().next() );
           }
           else if ( set.size() > 1 )
           {
               getLog().error( "found more then one artifact for given version: " + gem.getKey() + " " + gem.getValue() );
           }
        }
        
        File home = gemsConfig.getGemHome();
        // use gemHome as base for other gems installation directories
        String base = this.gemsConfig.getGemHome() != null ? 
                this.gemsConfig.getGemHome().getAbsolutePath() : 
                    (project.getBuild().getDirectory() + "/rubygems");
        try
        {
            final File gemHome;
            if ( "test".equals( scope ) || "provided".equals( scope ) )
            {
                gemHome = new File(base + "-" + scope);
            }
            else 
            {
                gemHome = new File( base );
            }
            this.gemsConfig.setGemHome(gemHome);
            this.gemsConfig.addGemPath(gemHome);
            
            getLog().info( "installing gem sets for " + scope + " scope into " + 
                           gemHome.getAbsolutePath().replace(project.getBasedir().getAbsolutePath() + File.separatorChar, "") );
            gemsInstaller.installGems( project, artifacts, null, (List<ArtifactRepository>) null);
           
        }
        finally
        {
            // reset old gem home again
            this.gemsConfig.setGemHome(home);
        }
    }
}
