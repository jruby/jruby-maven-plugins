package de.saumya.mojo.gem;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

public class GemRepositoryLayout implements ArtifactRepositoryLayout {
    // private static final char PATH_SEPARATOR = '/';
    private static final char GROUP_SEPARATOR    = '.';
    private static final char ARTIFACT_SEPARATOR = '-';

    public String getId() {
        return "gem";
    }

    public String pathOf(final Artifact artifact) {
        final StringBuffer path = new StringBuffer();

        if (!"rubygems".equals(artifact.getGroupId())) {
            path.append(artifact.getGroupId()).append(GROUP_SEPARATOR);
        }
        path.append(artifact.getArtifactId())
                .append(ARTIFACT_SEPARATOR)
                .append(artifact.getVersion());
        if (artifact.hasClassifier()) {
            path.append(ARTIFACT_SEPARATOR).append(artifact.getClassifier());
        }
        path.append(GROUP_SEPARATOR);

        final String extension = artifact.getArtifactHandler().getExtension();
        if ("pom".equals(extension)) {
            // just download the gem instead of the none existing pom
            path.append("gem");
        }
        else {
            path.append(extension);
        }

        return path.toString();
    }

    public String pathOfLocalRepositoryMetadata(
            final ArtifactMetadata metadata, final ArtifactRepository repository) {
        return pathOfRepositoryMetadata(metadata,
                                        metadata.getLocalFilename(repository));
    }

    private String pathOfRepositoryMetadata(final ArtifactMetadata metadata,
            final String filename) {
        final StringBuffer path = new StringBuffer();

        path.append(metadata.getGroupId()).append(GROUP_SEPARATOR);
        // .append(PATH_SEPARATOR);
        // if (!metadata.storedInGroupDirectory()) {
        // final String version = getBaseVersion(metadata);
        // // always store in version directory; default implementation does
        // // not
        // path.append(version).append(PATH_SEPARATOR);
        //
        path.append(metadata.getArtifactId()).append(GROUP_SEPARATOR);
        // }

        path.append(filename);

        return path.toString();
    }

    // /**
    // * Extract base version from metadata
    // *
    // * @param metadata
    // * @return base version from the artifact
    // */
    // private String getBaseVersion(final ArtifactMetadata metadata) {
    // String version = null;
    // if (metadata.getBaseVersion() != null) {
    // version = metadata.getBaseVersion();
    // }
    // else {
    // // artifact base version is not accessible from
    // // ArtifactRepositoryMetadata
    // if (metadata instanceof ArtifactRepositoryMetadata) {
    // try {
    // final Field f = metadata.getClass()
    // .getDeclaredField("artifact");
    // final boolean accessible = f.isAccessible();
    // try {
    // f.setAccessible(true);
    // final Artifact artifact = (Artifact) f.get(metadata);
    // version = artifact.getBaseVersion();
    // }
    // finally {
    // f.setAccessible(accessible);
    // }
    // }
    // catch (final Exception e) {
    // throw new RuntimeException(e);
    // }
    // }
    // }
    // return version;
    // }

    public String pathOfRemoteRepositoryMetadata(final ArtifactMetadata metadata) {
        return pathOfRepositoryMetadata(metadata, metadata.getRemoteFilename());
    }

    // private String formatAsDirectory(final String directory) {
    // return directory.replace(GROUP_SEPARATOR, PATH_SEPARATOR);
    // }
}
