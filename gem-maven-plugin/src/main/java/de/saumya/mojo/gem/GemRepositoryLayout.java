package de.saumya.mojo.gem;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

public class GemRepositoryLayout implements ArtifactRepositoryLayout {

    private static final char PATH_SEPARATOR     = '/';
    private static final char GROUP_SEPARATOR    = '.';
    private static final char ARTIFACT_SEPARATOR = '-';

    public String getId() {
        return "gem";
    }

    public String pathOf(final Artifact artifact) {
    	System.err.println( "pathOf(" + artifact + ")" );
        final StringBuffer path = new StringBuffer();

        if (!"rubygems".equals(artifact.getGroupId())) {
            // allow only rubygems groupIds from the repository

            // hack to generate some http error which let the download stop.
            // without it wagon will download the html with "page not found" and
            // stores it as artifact file
            return "../nonexistant-artifact";
        }
        path.append(artifact.getArtifactId())
                .append(ARTIFACT_SEPARATOR)
                .append(artifact.getVersion());
        if (artifact.hasClassifier()) {
            path.append(ARTIFACT_SEPARATOR).append(artifact.getClassifier());
        }
        path.append(GROUP_SEPARATOR);

        final String extension = artifact.getArtifactHandler().getExtension();
        if ("pom".equals(extension) || "gem".equals(extension)) {
            // just download the gem instead of the none existing pom
            path.append( extension );
        }
        else {
            // hack to generate some http error which let the download stop.
            // without it wagon will download the html with "page not found" and
            // stores it as jar file
            return "../nonexistant-artifact";
        }

        System.err.println( " --> " + path );
        return path.toString();
    }

    public String pathOfLocalRepositoryMetadata(
            final ArtifactMetadata metadata, final ArtifactRepository repository) {
        return pathOfRepositoryMetadata(metadata,
                                        metadata.getLocalFilename(repository));
    }

    private String formatAsDirectory(final String directory) {
        return directory.replace(GROUP_SEPARATOR, PATH_SEPARATOR);
    }

    private String pathOfRepositoryMetadata(final ArtifactMetadata metadata,
            final String filename) {
        final StringBuffer path = new StringBuffer();

        path.append(formatAsDirectory(metadata.getGroupId()))
                .append(PATH_SEPARATOR);
        if (!metadata.storedInGroupDirectory()) {
            path.append(metadata.getArtifactId()).append(PATH_SEPARATOR);

            if (metadata.storedInArtifactVersionDirectory()) {
                path.append(metadata.getBaseVersion()).append(PATH_SEPARATOR);
            }
        }

        path.append(filename);

        return path.toString();
    }

    public String pathOfRemoteRepositoryMetadata(final ArtifactMetadata metadata) {
        return pathOfRepositoryMetadata(metadata, metadata.getRemoteFilename());
    }
}
