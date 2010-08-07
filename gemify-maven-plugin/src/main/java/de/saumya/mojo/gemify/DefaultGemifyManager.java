/**
 * 
 */
package de.saumya.mojo.gemify;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.repository.legacy.metadata.DefaultMetadataResolutionRequest;
import org.apache.maven.repository.legacy.metadata.MetadataResolutionRequest;

//TODO make this a proper component
public class DefaultGemifyManager implements GemifyManager {

    RepositoryMetadataManager repositoryMetadataManager;

    ArtifactHandlerManager    artifactHandlerManager;

    Artifact resolveArtifact(final String gemName, final String version,
            final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories)
            throws MojoExecutionException {
        final Artifact artifact = createArtifact(gemName, version);

        if (version == null) {
            final List<String> versions = availableVersions(localRepository,
                                                            remoteRepositories,
                                                            artifact);
            artifact.setVersionRange(null);
            artifact.setVersion(versions.get(versions.size() - 1));
        }
        return artifact;
    }

    List<String> availableVersions(final String gemName,
            final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories)
            throws MojoExecutionException {
        final Artifact artifact = createArtifact(gemName, null);
        return availableVersions(localRepository, remoteRepositories, artifact);
    }

    private List<String> availableVersions(
            final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories,
            final Artifact artifact) throws MojoExecutionException {
        final MetadataResolutionRequest request = new DefaultMetadataResolutionRequest();
        request.setArtifact(artifact);
        request.setLocalRepository(localRepository);
        request.setRemoteRepositories(remoteRepositories);
        final RepositoryMetadata metadata = new ArtifactRepositoryMetadata(request.getArtifact());

        try {
            this.repositoryMetadataManager.resolve(metadata, request);
        }
        catch (final RepositoryMetadataResolutionException e) {
            throw new MojoExecutionException("error updateding versions of artifact: "
                    + artifact,
                    e);
        }

        final List<String> versions = metadata.getMetadata()
                .getVersioning()
                .getVersions();
        return versions;
    }

    private Artifact createArtifact(final String gemName, final String version)
            throws MojoExecutionException {
        final int index = gemName.lastIndexOf(".");
        final String groupId = gemName.substring(0, index);
        final String artifactId = gemName.substring(index + 1);

        final ArtifactHandler handler = this.artifactHandlerManager.getArtifactHandler("jar");
        Artifact artifact = null;
        try {
            artifact = new DefaultArtifact(groupId,
                    artifactId,
                    VersionRange.createFromVersionSpec(version == null
                            ? "[0.0.0,)"
                            : version),
                    "test",// scope
                    "jar",// type
                    null,// classifier
                    handler,
                    false);// optional
        }
        catch (final InvalidVersionSpecificationException e) {
            throw new MojoExecutionException("error parsing the version: "
                    + version, e);
        }
        return artifact;
    }
}