package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.ScriptException;
import org.apache.maven.project.MavenProject;

/**
 * installs a set of given gems without resolving any transitive dependencies
 */
@Mojo( name = "sets", defaultPhase = LifecyclePhase.INITIALIZE )
public class SetsMojo extends AbstractGemMojo {
    
    /**
     * the scope under which the gems get installed
     */
    @Parameter( defaultValue = "compile" )
    protected String scope;

    /**
     * map of gemname to version, i.e. it is a "list" of gems with fixed version
     */
    @Parameter
    protected Map<String, String>  gems = Collections.emptyMap();

    @Component
    protected ModelReader reader;

    protected void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException, GemException {
        Set<Artifact> gems = new TreeSet<Artifact>();
        Set<Artifact> jars = new TreeSet<Artifact>();
        for( Map.Entry<String, String> gem : this.gems.entrySet() ) {
            Set<Artifact> set = manager.resolve( manager.createGemArtifact( gem.getKey(),
                                                                            gem.getValue() ),
                                               localRepository,
                                               project.getRemoteArtifactRepositories() );
           if ( set.size() == 1 )
           {
               Artifact artifact = set.iterator().next();
               artifact.setScope(scope);
               gems.add(artifact);
               collectJarDependencies(jars, artifact);
           }
           else if ( set.size() > 1 )
           {
               getLog().error( "found more then one artifact for given version: " + gem.getKey() + " " + gem.getValue() );
           }
        }

        resolveJarDepedencies(jars);

        installGems(gems);

        Set<Artifact> resolved = (Set<Artifact>) project.getContextValue("jruby.resolved.artifacts");
        if (resolved == null) {
            resolved = new LinkedHashSet<Artifact>();
            project.setContextValue("jruby.resolved.artifacts", resolved);
        }
        resolved.addAll(jars);
        resolved.addAll(gems);
        project.setResolvedArtifacts(resolved);
    }

    private void installGems(Set<Artifact> gems) throws IOException, ScriptException, GemException {
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
                gemHome = gemHome( base, scope);
            }
            else
            {
                gemHome = new File( base );
            }
            this.gemsConfig.setGemHome(gemHome);
            this.gemsConfig.addGemPath(gemHome);

            getLog().info("installing gem sets for " + scope + " scope into " +
                    gemHome.getAbsolutePath().replace(project.getBasedir().getAbsolutePath() + File.separatorChar, ""));
            gemsInstaller.installGems(project, gems, null, (List<ArtifactRepository>) null);
        }
        finally
        {
            // reset old gem home again
            this.gemsConfig.setGemHome(home);
        }
    }

    private void collectJarDependencies(Set<Artifact> jars, Artifact artifact) throws GemException, IOException {
        Set<Artifact> set = manager.resolve(manager.createArtifact(artifact.getGroupId(),
                        artifact.getArtifactId(), artifact.getVersion(), "pom"),
                localRepository,
                project.getRemoteArtifactRepositories());
        Model pom = reader.read(set.iterator().next().getFile(), null);
        for( Dependency dependency : pom.getDependencies() ){
            if (!dependency.getType().equals("gem")) {
                if (dependency.getScope() == null || dependency.getScope().equals("compile") || dependency.equals("runtime")) {
                    Artifact a = manager.createArtifact(dependency.getGroupId(), dependency.getArtifactId(),
                            dependency.getVersion(), dependency.getClassifier(), dependency.getType());
                    a.setScope(dependency.getScope());
                    jars.add(a);
                }
            }
        }
    }

    private void resolveJarDepedencies(Set<Artifact> jars) {
        ArtifactResolutionRequest req = new ArtifactResolutionRequest()
                .setArtifact(project.getArtifact())
                .setResolveRoot(false)
                .setArtifactDependencies(jars)
                .setResolveTransitively(true)
                .setLocalRepository(localRepository)
                .setRemoteRepositories(project.getRemoteArtifactRepositories());
        ArtifactResolutionResult result = this.repositorySystem.resolve(req);
        Set<Artifact> resolvedArtifacts = result.getArtifacts();
        for( Artifact artifact : resolvedArtifacts ){
            // * compile scope we leave things as they are
            // * other scopes we only take runtime, compile time artifacts and set the scope to outer scope
            if ("compile".equals(scope)) {
                if (artifact.getScope() == null) artifact.setScope(scope);
                jars.add(artifact);
            }
            else if (!"test".equals(artifact.getScope()) && !"provided".equals(artifact.getScope())) {
                artifact.setScope(scope);
                jars.add(artifact);
            }
        }
    }
}
