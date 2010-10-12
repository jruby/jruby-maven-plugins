package de.saumya.mojo.ruby.gems;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystemSession;

public interface GemManager {

    // GEM artifact factory methods
    public abstract Artifact createGemArtifact(final String gemname)
            throws GemException;

    public abstract Artifact createGemArtifact(final String gemname,
            final String version) throws GemException;

    public abstract Artifact createGemArtifactWithLatestVersion(
            final String gemname, final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories)
            throws GemException;

    // GEM repositories
    public abstract ArtifactRepository defaultGemArtifactRepository();

    public ArtifactRepository defaultGemArtifactRepositoryForVersion(
            final String artifactVersion);

    public void addDefaultGemRepository(final List<ArtifactRepository> repos);

    public void addDefaultGemRepositoryForVersion(final String artifactVersion,
            final List<ArtifactRepository> repos);

    // jar artifacts for GEMNAME (maven-gems)

    public abstract Artifact createJarArtifactForGemname(final String gemName)
            throws GemException;

    public abstract Artifact createJarArtifactForGemname(final String gemName,
            final String version) throws GemException;

    public abstract Artifact createJarArtifactForGemnameWithLatestVersion(
            final String gemName, final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories)
            throws GemException;

    // convenience methods for artifacts
    public void resolve(final Artifact artifact,
            final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories)
            throws GemException;

    public Artifact createArtifact(final String groupId,
            final String artifactId, final String version, final String type);

    public MavenProject buildPom(Artifact artifact,
            final RepositorySystemSession repositorySystemSession,
            final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories)
            throws GemException;

    // versions
    public List<String> availableVersions(final Artifact artifact,
            final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories)
            throws GemException;

    public String latestVersion(final Artifact artifact,
            final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories)
            throws GemException;

}