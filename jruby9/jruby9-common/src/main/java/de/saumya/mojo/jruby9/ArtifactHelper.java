package de.saumya.mojo.jruby9;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.util.FileUtils;

public class ArtifactHelper {
    private final UnArchiver archiver;
    private final RepositorySystem system;
    private final ArtifactRepository localRepo;
    private final List<ArtifactRepository> remoteRepos;

    public ArtifactHelper(UnArchiver archiver, RepositorySystem system,
            ArtifactRepository localRepo, List<ArtifactRepository> remoteRepos) {
        this.system = system;
        this.localRepo = localRepo;
        this.remoteRepos = remoteRepos;
        this.archiver = archiver;
    }

    public Set<Artifact> resolve(String groupId, String artifactId, String version)
            throws MojoExecutionException {
        return resolve(groupId, artifactId, version, null);
    }

    public Set<Artifact> resolve(String groupId, String artifactId, String version, final String exclusion)
            throws MojoExecutionException {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setResolveTransitively(true)
            .setResolveRoot(true)
            .setArtifact(system.createArtifact(groupId, artifactId, version, "jar"))
            .setLocalRepository(this.localRepo)
            .setRemoteRepositories(remoteRepos).setCollectionFilter(new ArtifactFilter() {
                
                @Override
                public boolean include(Artifact artifact) {
                    if (exclusion != null && 
                            (artifact.getGroupId() + ":" + artifact.getArtifactId()).equals(exclusion)) {
                        return false;
                    }
                    return artifact.getScope() == null || artifact.getScope().equals("compile") || artifact.getScope().equals("runtime");
                }
            });

        ArtifactResolutionResult result = system.resolve(request);  
        // TODO error handling
        return result.getArtifacts();
    }

    public void copy(File output, String groupId, String artifactId, String version)
            throws MojoExecutionException {
        copy(output, groupId, artifactId, version, null);
    }

    public void copy(File output, String groupId, String artifactId, String version, String exclusion)
            throws MojoExecutionException {
        output.mkdirs();
        for(Artifact artifact: resolve(groupId, artifactId, version, exclusion)) {
            try {
                FileUtils.copyFile(artifact.getFile(), new File(output, artifact.getFile().getName()));
            } catch (IOException e) {
                throw new MojoExecutionException("could not copy: " + artifact, e);
            }
        }
    }

    public void unzip(File output, String groupId, String artifactId, String version)
            throws MojoExecutionException {
        output.mkdirs();
        archiver.setDestDirectory(output);
        for(Artifact artifact: resolve(groupId, artifactId, version)) {
            archiver.setSourceFile(artifact.getFile());
            try {
                archiver.extract();
            } catch (ArchiverException e) {
                throw new MojoExecutionException("could not unzip: " + artifact, e);
            }
        }
    }
}