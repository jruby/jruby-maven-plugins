package de.saumya.mojo.jruby9;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
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
    private final File target;
    
    public ArtifactHelper(String outputDirectory, UnArchiver archiver, RepositorySystem system,
            ArtifactRepository localRepo, List<ArtifactRepository> remoteRepos) {
        this.system = system;
        this.localRepo = localRepo;
        this.remoteRepos = remoteRepos;
        this.archiver = archiver;
        
        target = new File(outputDirectory);
        target.mkdirs();

        archiver.setDestDirectory(target);
    }

    public Set<Artifact> resolve(String groupId, String artifactId, String version)
            throws MojoExecutionException {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
        .setArtifact(system.createArtifact(groupId, artifactId, version, "jar"))
        .setLocalRepository(this.localRepo)
        .setRemoteRepositories(remoteRepos);

        ArtifactResolutionResult result = system.resolve(request);  
        // TODO error handling
        return result.getArtifacts();
    }

    public void copy(String groupId, String artifactId, String version)
            throws MojoExecutionException {
        for(Artifact artifact: resolve(groupId, artifactId, version)) {
            try {
                FileUtils.copyFile(artifact.getFile(), new File(target, artifact.getFile().getName()));
            } catch (IOException e) {
                throw new MojoExecutionException("could not copy: " + artifact, e);
            }
        }
    }

    public void unzip(String groupId, String artifactId, String version)
            throws MojoExecutionException {
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