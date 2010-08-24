package de.saumya.mojo.ruby;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

public interface GemifyManager {

    public abstract Artifact createGemArtifact(final String artifactId,
            final String version) throws GemException;

    public abstract Artifact createArtifact(final String gemName,
            final String version, final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories)
            throws GemException;

    public abstract Artifact createArtifact(final String groupId,
            final String artifactId, final String version,
            final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories)
            throws GemException;

    public abstract Artifact createArtifact(final Artifact artifact,
            final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories)
            throws GemException;

    public ArtifactRepository defaultGemArtifactRepositoryForVersion(
            final String artifactVersion);

    public void addDefaultGemRepositoryForVersion(final String artifactVersion,
            final List<ArtifactRepository> repos);

    public List<String> availableVersions(final String gemName,
            final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories)
            throws GemException;

    public List<String> availableVersions(final Artifact artifact,
            final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories)
            throws GemException;

}