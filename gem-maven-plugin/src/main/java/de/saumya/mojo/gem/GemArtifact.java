/**
 *
 */
package de.saumya.mojo.gem;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.MavenProject;

@SuppressWarnings("deprecation")
public class GemArtifact implements Artifact {

    // helper to make maven2 behave like maven3
    static class GemArtifactHandler implements ArtifactHandler {

        private final ArtifactHandler handler;

        GemArtifactHandler(final ArtifactHandler handler) {
            this.handler = handler;
        }

        public String getClassifier() {
            return this.handler.getClassifier();
        }

        public String getDirectory() {
            return this.handler.getDirectory();
        }

        public String getExtension() {
            if (this.handler.getExtension().equals("java-gem")) {
                return "gem";
            }
            else {
                return this.handler.getExtension();
            }
        }

        public String getLanguage() {
            return this.handler.getLanguage();
        }

        public String getPackaging() {
            return this.handler.getPackaging();
        }

        public boolean isAddedToClasspath() {
            return this.handler.isAddedToClasspath();
        }

        public boolean isIncludesDependencies() {
            return this.handler.isIncludesDependencies();
        }
    }

    private final Artifact artifact;
    private final File     jarFile;

    public GemArtifact(final MavenProject project) {
        this.artifact = project.getArtifact();
        this.jarFile = this.artifact.getFile();
        if (isGem()) {
            this.artifact.setFile(new File(new File(project.getBuild()
                    .getDirectory()), getGemFile()));
        }
        // allow maven2 to do the right thing with the classifier
        project.setArtifact(this);
        this.artifact.setArtifactHandler(new GemArtifactHandler(this.artifact.getArtifactHandler()));
    }

    public String getGemName() {
        if (getGroupId().equals("rubygems")) {
            return getArtifactId();
        }
        else {
            final StringBuilder name = new StringBuilder(getGroupId());
            name.append(".").append(getArtifactId());
            return name.toString();
        }
    }

    public String getGemVersion() {
        return getGemVersion(getVersion());
    }

    public static String getGemVersion(final String artifactVersion) {
        final StringBuilder version = new StringBuilder();
        boolean first = true;
        for (final String part : artifactVersion.replaceAll("-SNAPSHOT", "")
                .replace("-", ".")
                .split("\\.")) {
            if (part.length() > 0) {
                if (first) {
                    first = false;
                    version.append(part);
                }
                else {
                    version.append(".").append(part);
                }
            }
        }
        return version.toString();
    }

    public String getGemFile() {
        final StringBuilder name = new StringBuilder(getGemName());
        name.append("-").append(getGemVersion());
        if (hasJarFile()) {
            name.append("-java");
        }
        name.append(".gem");
        return name.toString();
    }

    public File getFile() {
        return this.artifact.getFile();
    }

    public String getClassifier() {
        return this.artifact.getClassifier();
    }

    public boolean hasJarFile() {
        return "java-gem".equals(getType());
    }

    public File getJarFile() {
        if (hasJarFile()) {
            return this.jarFile;
        }
        else {
            return null;
        }
    }

    public void addMetadata(final ArtifactMetadata metadata) {
        this.artifact.addMetadata(metadata);
    }

    public int compareTo(final Artifact o) {
        return this.artifact.compareTo(o);
    }

    public ArtifactHandler getArtifactHandler() {
        return this.artifact.getArtifactHandler();
    }

    public String getArtifactId() {
        return this.artifact.getArtifactId();
    }

    public List<ArtifactVersion> getAvailableVersions() {
        return this.artifact.getAvailableVersions();
    }

    public String getBaseVersion() {
        return this.artifact.getBaseVersion();
    }

    // public String getClassifier() {
    // return this.artifact.getClassifier();
    // }

    public String getDependencyConflictId() {
        return this.artifact.getDependencyConflictId();
    }

    public ArtifactFilter getDependencyFilter() {
        return this.artifact.getDependencyFilter();
    }

    public List<String> getDependencyTrail() {
        return this.artifact.getDependencyTrail();
    }

    public String getDownloadUrl() {
        return this.artifact.getDownloadUrl();
    }

    public String getGroupId() {
        return this.artifact.getGroupId();
    }

    public String getId() {
        return this.artifact.getId();
    }

    public Collection<ArtifactMetadata> getMetadataList() {
        return this.artifact.getMetadataList();
    }

    public ArtifactRepository getRepository() {
        return this.artifact.getRepository();
    }

    public String getScope() {
        return this.artifact.getScope();
    }

    public ArtifactVersion getSelectedVersion()
            throws OverConstrainedVersionException {
        return this.artifact.getSelectedVersion();
    }

    public String getType() {
        return this.artifact.getType();
    }

    public String getVersion() {
        return this.artifact.getVersion();
    }

    public VersionRange getVersionRange() {
        return this.artifact.getVersionRange();
    }

    public boolean hasClassifier() {
        return getClassifier() != null;
    }

    // public boolean hasClassifier() {
    // return this.artifact.hasClassifier();
    // }

    public boolean isOptional() {
        return this.artifact.isOptional();
    }

    public boolean isRelease() {
        return this.artifact.isRelease();
    }

    public boolean isResolved() {
        return this.artifact.isResolved();
    }

    public boolean isSelectedVersionKnown()
            throws OverConstrainedVersionException {
        return this.artifact.isSelectedVersionKnown();
    }

    public boolean isSnapshot() {
        return this.artifact.isSnapshot();
    }

    public void selectVersion(final String version) {
        this.artifact.selectVersion(version);
    }

    public void setArtifactHandler(final ArtifactHandler handler) {
        this.artifact.setArtifactHandler(handler);
    }

    public void setArtifactId(final String artifactId) {
        this.artifact.setArtifactId(artifactId);
    }

    public void setAvailableVersions(final List<ArtifactVersion> versions) {
        this.artifact.setAvailableVersions(versions);
    }

    public void setBaseVersion(final String baseVersion) {
        this.artifact.setBaseVersion(baseVersion);
    }

    public void setDependencyFilter(final ArtifactFilter artifactFilter) {
        this.artifact.setDependencyFilter(artifactFilter);
    }

    public void setDependencyTrail(final List<String> dependencyTrail) {
        this.artifact.setDependencyTrail(dependencyTrail);
    }

    public void setDownloadUrl(final String downloadUrl) {
        this.artifact.setDownloadUrl(downloadUrl);
    }

    public void setFile(final File destination) {
        this.artifact.setFile(destination);
    }

    public void setGroupId(final String groupId) {
        this.artifact.setGroupId(groupId);
    }

    public void setOptional(final boolean optional) {
        this.artifact.setOptional(optional);
    }

    public void setRelease(final boolean release) {
        this.artifact.setRelease(release);
    }

    public void setRepository(final ArtifactRepository remoteRepository) {
        this.artifact.setRepository(remoteRepository);
    }

    public void setResolved(final boolean resolved) {
        this.artifact.setResolved(resolved);
    }

    public void setResolvedVersion(final String version) {
        this.artifact.setResolvedVersion(version);
    }

    public void setScope(final String scope) {
        this.artifact.setScope(scope);
    }

    public void setVersion(final String version) {
        this.artifact.setVersion(version);
    }

    public void setVersionRange(final VersionRange newRange) {
        this.artifact.setVersionRange(newRange);
    }

    public void updateVersion(final String version,
            final ArtifactRepository localRepository) {
        this.artifact.updateVersion(version, localRepository);
    }

    @Override
    public String toString() {
        return this.artifact.toString();
    }

    public boolean isGem() {
        return this.artifact.getType().contains("gem");
    }
}
