/**
 * 
 */
package de.saumya.mojo.ruby;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.repository.legacy.metadata.DefaultMetadataResolutionRequest;
import org.apache.maven.repository.legacy.metadata.MetadataResolutionRequest;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component(role = GemifyManager.class)
public class DefaultGemifyManager implements GemifyManager {

    @Requirement
    RepositoryMetadataManager repositoryMetadataManager;

    @Requirement
    ArtifactHandlerManager    artifactHandlerManager;

    public ArtifactRepository defaultGemArtifactRepositoryForVersion(
            final String artifactVersion) {
        final String preRelease = artifactVersion != null
                && artifactVersion.matches(".*[a-z][A-Z].*") ? "pre" : "";
        return new MavenArtifactRepository("rubygems-" + preRelease
                + "releases",
                "http://gems.saumya.de/" + preRelease + "releases",
                new DefaultRepositoryLayout(),
                new ArtifactRepositoryPolicy(),
                new ArtifactRepositoryPolicy());
    }

    public void addDefaultGemRepositoryForVersion(final String artifactVersion,
            final List<ArtifactRepository> repos) {
        final ArtifactRepository repo = defaultGemArtifactRepositoryForVersion(artifactVersion);
        for (final ArtifactRepository ar : repos) {
            if (ar.getUrl().equals(repo.getUrl())) {
                return;
            }
        }
        repos.add(repo);
    }

    public Artifact createGemArtifact(final String artifactId,
            final String version) throws GemException {
        return createArtifact("rubygems", artifactId, version, "gem", "runtime");
    }

    public Artifact createArtifact(final String gemName, final String version,
            final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories)
            throws GemException {
        return createArtifact(createArtifactFromGemname(gemName, version),
                              localRepository,
                              remoteRepositories);
    }

    public Artifact createArtifact(final String groupId,
            final String artifactId, final String version,
            final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories)
            throws GemException {
        return createArtifact(createArtifact(groupId,
                                             artifactId,
                                             version,
                                             "jar",
                                             "test"),
                              localRepository,
                              remoteRepositories);
    }

    public Artifact createArtifact(final Artifact artifact,
            final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories)
            throws GemException {
        if (artifact.getVersion() == null) {
            final List<String> versions = availableVersions(artifact,
                                                            localRepository,
                                                            remoteRepositories);
            artifact.setVersionRange(null);
            artifact.setVersion(versions.get(versions.size() - 1));
        }
        return artifact;
    }

    public List<String> availableVersions(final String gemName,
            final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories)
            throws GemException {
        final Artifact artifact = createArtifactFromGemname(gemName, null);
        return availableVersions(artifact, localRepository, remoteRepositories);
    }

    public List<String> availableVersions(final Artifact artifact,
            final ArtifactRepository localRepository,
            final List<ArtifactRepository> remoteRepositories)
            throws GemException {
        final MetadataResolutionRequest request = new DefaultMetadataResolutionRequest();
        request.setArtifact(artifact);
        request.setLocalRepository(localRepository);
        request.setRemoteRepositories(remoteRepositories);
        final RepositoryMetadata metadata = new ArtifactRepositoryMetadata(request.getArtifact());

        try {
            this.repositoryMetadataManager.resolve(metadata, request);
        }
        catch (final RepositoryMetadataResolutionException e) {
            throw new GemException("error updateding versions of artifact: "
                    + artifact, e);
        }

        final List<String> versions = metadata.getMetadata()
                .getVersioning()
                .getVersions();
        return versions;
    }

    Artifact createArtifactFromGemname(final String gemName,
            final String version) throws GemException {
        final int index = gemName.lastIndexOf(".");
        final String groupId = gemName.substring(0, index);
        final String artifactId = gemName.substring(index + 1);
        return createArtifact(groupId, artifactId, version, "jar", "test");
    }

    private Artifact createArtifact(final String groupId,
            final String artifactId, final String version, final String type,
            final String scope) throws GemException {

        final ArtifactHandler handler = this.artifactHandlerManager.getArtifactHandler(type);
        Artifact artifact = null;
        try {
            artifact = new DefaultArtifact(groupId,
                    artifactId,
                    VersionRange.createFromVersionSpec(version == null
                            ? "[0.0.0,)"
                            : version),
                    scope,
                    type,
                    null,// classifier
                    handler,
                    false);// optional
        }
        catch (final InvalidVersionSpecificationException e) {
            throw new GemException("error parsing the version: " + version,
                    e);
        }
        return artifact;
    }
}