package de.saumya.mojo.gemify;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;

import de.saumya.mojo.gems.Maven2GemVersionConverter;
import de.saumya.mojo.gems.MavenArtifactConverter;
import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.gems.GemManager;

/**
 * Goal which list the versions for the given gemname.
 * 
 * @goal versions
 * @requiresProject false
 */
public class VersionsMojo extends AbstractGemifyMojo {

    /** @component */
    private ProjectBuilder          builder;

    /** @component */
    private GemManager            manager;

    protected void executeGemify() throws MojoExecutionException {
        try {
            // first get all maven-versions
            final Artifact artifact = this.manager.createPomArtifactForGemname(this.gemname);
            // use the remoteRepositories list from parent since that list obeys offline mode
            final List<String> versions = this.manager.availableVersions(artifact,
                                                                         this.localRepository,
                                                                         this.remoteRepositories);
            getLog().debug("raw versions: " + versions);
            // now convert the maven-versions into gem-versions
            final List<String> gemVersions = new ArrayList<String>(versions.size());
            final Maven2GemVersionConverter converter = new Maven2GemVersionConverter();
            for (final String version : versions) {
                final ProjectBuildingRequest request = new DefaultProjectBuildingRequest();
                request.setLocalRepository(this.localRepository)
                    .setRemoteRepositories(this.remoteRepositories)
                    .setResolveDependencies(false)
                    .setRepositorySession(this.repositorySession)
                    .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
                try {
                    artifact.setVersion(version);
                    // build the POM for that artifact to ensure that this
                    // pom works when using that version for building a gem
                    // i.e.  org.slf4j.slf4j:log4j12:1.1.0-RC0
                    // has no parent-pom in the central repository
                    ProjectBuildingResult result = this.builder.build(artifact, request);
                    // assume that maven central has no broken poms ;-)
                    if(this.remoteRepositories.size() > 1){
                        for (org.apache.maven.model.Dependency dep : result.getProject()
                                .getDependencies()) {
                            // skip version ranges
                            if(!version.matches("[\\[\\](),]")){
                                Artifact a = this.manager.createArtifact(dep.getGroupId(),
                                                                         dep.getArtifactId(),
                                                                         dep.getVersion(),
                                                                         "pom");
                                this.builder.build(a, request);
                            }
                        }
                    }
                    gemVersions.add(converter.createGemVersion(version));
                }
                catch (final ProjectBuildingException e) {
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("skip version (pom does not load): "
                                               + version,
                                       e);
                    }
                    else {
                        getLog().info("skip version (pom does not load): "
                                + version);
                    }
                }
            }
            // print result for user
            getLog().info("\n\n\t" + MavenArtifactConverter.GEMNAME_PREFIX + this.gemname + " " + gemVersions + "\n\n");
        }
        catch (final GemException e) {
            throw new MojoExecutionException("error finding versions for: "
                    + this.gemname, e);
        }
    }
}
