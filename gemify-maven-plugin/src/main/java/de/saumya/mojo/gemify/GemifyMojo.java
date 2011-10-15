package de.saumya.mojo.gemify;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;

import de.saumya.mojo.gems.ArtifactCoordinates;
import de.saumya.mojo.gems.GemArtifact;
import de.saumya.mojo.gems.Maven2GemVersionConverter;
import de.saumya.mojo.gems.MavenArtifact;
import de.saumya.mojo.gems.MavenArtifactConverter;
import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.gems.GemManager;

/**
 * Goal which takes an maven artifact and converts it and its jar dependencies
 * to gem.
 * 
 * @goal gemify
 * @requiresProject false
 */
public class GemifyMojo extends AbstractGemifyMojo {

    private static final String SEPARATOR = "------------------------";

    /**
     * the version of the maven artifact which gets gemified.
     * 
     * @parameter default-value="${gemify.version}"
     */
    private String                          version;

    /**
     * @parameter default-value="${gemify.tempDir}"
     */
    private File                            tempDir;

    /**
     * gemify development depencendies as well. default: false
     * 
     * @parameter default-value="${gemify.development}"
     */
    private boolean                         development;

    /** @parameter default-value="${gemify.skipDependencies}" */
    private boolean                         skipDependencies;

    /** @parameter default-value="${gemify.onlySpecs}" */
    private boolean                         onlySpecs;

    /** @component */
    private ProjectBuilder                  builder;

    /** @component */
    private MavenArtifactConverter          converter;

    /** @component */
    private GemManager            manager;

    public void executeGemify() throws MojoExecutionException {

        ProjectBuildingResult result = buildProject(this.gemname,
                                                    this.version,
                                                    this.onlySpecs);

        MavenProject origin = result.getProject();
        
        if (origin.getDistributionManagement() != null){
            Relocation relocation = result.getProject().getDistributionManagement().getRelocation();
            if(relocation != null){
                StringBuilder relocatedGemname = new StringBuilder();
                relocatedGemname.append(relocation.getGroupId() == null
                        ? origin.getGroupId()
                        : relocation.getGroupId());
                relocatedGemname.append(GemManager.GROUP_ID_ARTIFACT_ID_SEPARATOR);
                relocatedGemname.append(relocation.getArtifactId() == null
                        ? origin.getArtifactId()
                        : relocation.getArtifactId());
                // take the relocated project to go over the dependencies
                result = buildProject(relocatedGemname.toString(),
                                      relocation.getVersion() == null
                                              ? origin.getVersion()
                                              : relocation.getVersion(),
                                      this.onlySpecs);
            }
        }
        
        // visit all dependencies
        final Map<String, Node> visited = new HashMap<String, Node>();
        visit(visited,
              result,
              new Node(keyOf(result.getProject().getArtifact()),
                      result.getProject()));

        // gemify all dependencies as desired
        if (!this.onlySpecs && !this.skipDependencies) {
            for (final org.sonatype.aether.graph.Dependency artifact : result.getDependencyResolutionResult()
                    .getResolvedDependencies()) {
                if (inScope(artifact.getScope(), true)) {
                    final Node node = visited.get(keyOf(artifact));
                    if (node != null) {
                        gemifyMavenProject(visited.get(keyOf(artifact)).project);
                        visited.remove(keyOf(artifact));
                    }
                    else {
                        getLog().debug("skip optional " + artifact);
                    }
                }
            }
        }

        // gemify actual artifact for given gemname
        if(origin != result.getProject() && !this.skipDependencies){
            gemifyMavenProject(result.getProject());
        }
        gemifyMavenProject(origin);

        // gemify orphaned dependencies
        if (!this.skipDependencies && !this.onlySpecs) {
            for (final Map.Entry<String, Node> entry : visited.entrySet()) {
                gemifyMavenProject(entry.getValue().project);
            }
        }
    }

    /**
     * 
     * @param gemName
     *            if given then: groupId == null and artifactId == null
     * @param version
     * @param onlyPom
     *            TODO
     * @param groupId
     *            if given then: gemName == null and artifactId != null
     * @param artifactId
     *            if given then: gemName == null and groupId != null
     * @return
     * @throws MojoExecutionException
     */
    private ProjectBuildingResult buildProject(final String gemname,
            final String version, final boolean onlyPom)
            throws MojoExecutionException {
        try {
            Artifact artifact = null;
            if (version == null) {
                // use the remoteRepositories list from parent since that list obeys offline mode
                artifact = this.manager.createJarArtifactForGemnameWithLatestVersion(gemname,
                                                                                     this.localRepository,
                                                                                     this.remoteRepositories);
            }
            else {
                // find the latest version
                // use the remoteRepositories list from parent since that list obeys offline mode
                final List<String> versions = this.manager.availableVersions(this.manager.createJarArtifactForGemname(this.gemname),
                                                                             this.localRepository,
                                                                             this.remoteRepositories);

                // the given version is the gem version, so find the respective
                // maven-version
                final Maven2GemVersionConverter converter = new Maven2GemVersionConverter();
                for (final String v : versions) {
                    if (version.equals(converter.createGemVersion(v))) {
                        artifact = this.manager.createJarArtifactForGemname(gemname, v);
                        break;
                    }
                }
                // did not find it ? then assume the given version is a maven-version
                if (artifact == null) {
                    artifact = this.manager.createJarArtifactForGemname(gemname,
                                                                           version);
                }
            }
            return buildProject(artifact, onlyPom);
        }
        catch (final GemException e) {
            throw new MojoExecutionException("Error creating artifact when gemifying: "
                    + gemname,
                    e);
        }
    }

    private ProjectBuildingResult buildProject(final String groupId,
            final String artifactId, final String version)
            throws MojoExecutionException {
        final Artifact artifact = this.manager.createArtifact(groupId,
                                                                 artifactId,
                                                                 version,
                                                                 "jar");

        return buildProject(artifact, false);
    }

    private ProjectBuildingResult buildProject(Artifact artifact,
            final boolean isPom) throws MojoExecutionException {
        final ProjectBuildingResult result = buildMavenProject(artifact, !isPom);

        if (result.getProject().getDistributionManagement() != null) {
            final Relocation relocation = result.getProject()
                    .getDistributionManagement()
                    .getRelocation();
            if (relocation != null) {
                String newGroupId = relocation.getGroupId() == null
                        ? artifact.getGroupId()
                        : relocation.getGroupId();
                String newArtifactId = relocation.getArtifactId() == null
                        ? artifact.getArtifactId()
                        : relocation.getArtifactId();
                String newVersion = relocation.getVersion() == null
                        ? artifact.getVersion()
                        : relocation.getVersion();
                Artifact a = result.getProject().getArtifact();
                result.getProject()
                        .setArtifact(this.manager.createArtifact(a.getGroupId(),
                                                                 a.getArtifactId(),
                                                                 a.getVersion(),
                                                                 "pom"));
                getLog().info("\n\n\tartifact " + artifact + " is relocated to "
                        + newGroupId
                        + ":"
                        + newArtifactId
                        + " version="
                        + newVersion
                        + (relocation.getMessage() == null ? "" : " "
                                + relocation.getMessage())
                        + "\n\ttry to use the relocated artifact\n\n");
                }
        }
        return result;
    }

    static class Node {
        final MavenProject project;
        final int          depth;
        Node               parent;
        Set<Node>          children = new HashSet<Node>();
        final String       id;

        Node(final String id, final MavenProject project) {
            this(null, id, project);
        }

        Node(final Node parent, final String id, final MavenProject project) {
            this.parent = parent;
            this.id = id;
            this.project = project;
            this.depth = parent == null ? 0 : parent.depth + 1;
        }

        void removeParent() {
            if (this.parent != null) {
                this.parent.children.remove(this);
            }
            this.parent = null;
        }

        boolean isOrphaned() {
            if (this.parent == null) {
                return this.depth > 0;
            }
            else {
                return this.parent.isOrphaned();
            }
        }

        @Override
        public int hashCode() {
            return this.project.hashCode();
        }

        @Override
        public boolean equals(final Object other) {
            return this.project.equals(((Node) other).project);
        }
    }

    private void visit(final Map<String, Node> visited,
            final ProjectBuildingResult build, final Node parent)
            throws MojoExecutionException {
        getLog().debug("visit --- " + parent.id + " ---"
                + build.getProject().getArtifact().getVersion());

        final boolean isRoot = visited.isEmpty();
        visited.put(parent.id, parent);
        for (final Dependency dep : build.getProject().getDependencies()) {
            getLog().debug("      --- " + dep + " from " + parent.id);

            final String depId = keyOf(dep);
            // obey the development flag only for the root project
            if (inScope(dep.getScope(), isRoot) && !dep.isOptional()) {
                final Node v = visited.get(depId);
                if (v == null || v.depth > parent.depth + 1) {
                    if (v != null) {
                        v.removeParent();
                    }
                    final ProjectBuildingResult buildChild = buildProject(dep.getGroupId(),
                                                                          dep.getArtifactId(),
                                                                          dep.getVersion());
                    visit(visited, buildChild, new Node(parent,
                            keyOf(dep),
                            buildChild.getProject()));
                }
            }
        }
    }

    private String keyOf(final org.sonatype.aether.graph.Dependency depencency) {
        return depencency.getArtifact().getGroupId() + ":"
                + depencency.getArtifact().getArtifactId();
    }

    private String keyOf(final Dependency depencency) {
        return depencency.getGroupId() + ":" + depencency.getArtifactId();
    }

    private String keyOf(final Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    private boolean inScope(final String scope, final boolean obeyDevelopment) {
        return (obeyDevelopment && this.development) || "compile".equals(scope)
                || "runtime".equals(scope);
    }

    private File gemifyMavenProject(final MavenProject pom)
            throws MojoExecutionException {
        getLog().debug("gemify " + pom + pom.getPackaging());

        if (pom.getArtifact().getFile() == null
                || !pom.getArtifact().getFile().exists()) {
            final ArtifactResolutionRequest request = new ArtifactResolutionRequest();
            request.setArtifact(pom.getArtifact())
                    .setLocalRepository(this.localRepository)
                    .setRemoteRepositories(this.remoteRepositories)
                    .setResolveRoot(!this.onlySpecs)
                    .setResolveTransitively(false)
                    .setOffline(repositorySession.isOffline());
            final ArtifactResolutionResult result = this.repositorySystem.resolve(request);
            if (result.getMissingArtifacts().size() > 0) {
                // prepare the exception message
                StringBuilder buf = new StringBuilder();
                for(Artifact artifact: result.getMissingArtifacts()){
                    buf.append("\n\nMissing Artifacts:\n").append(SEPARATOR);
                    try {
                        MavenProject model = manager.buildModel(artifact,
                                                                this.repositorySession,
                                                                this.localRepository,
                                                                this.remoteRepositories,
                                                                false);
                        String url = model.getDistributionManagement() == null ?
                                null :
                                model.getDistributionManagement().getDownloadUrl();
                        buf.append("\nArtifact: ").append(artifact).append("\n\n");

                        if(url != null){
                            buf.append("Try downloading the file manually from:\n\t").append(url);
                            buf.append("\n\nThen, install it using the command:\n\t");
                        }
                        else {
                            getLog().debug("no download url for " + artifact);
                        }
                        buf.append("mvn install:install-file -DgroupId=").append(artifact.getGroupId())
                            .append(" -DartifactId=").append(artifact.getArtifactId())
                            .append(" -Dversion=").append(artifact.getVersion())
                            .append(" -Dpackaging=jar -Dfile=/path/to/file");
                    }
                    catch (GemException e) {
                        getLog().warn("error building pom for " + artifact, e);
                    }
                    buf.append("\n\n").append(SEPARATOR).append("\n\n");
                }

                throw new MojoExecutionException((this.repositorySession.isOffline()
                        ? "The repository system is offline. "
                        : "")
                        + buf);
            }
        }
        final MavenArtifact mavenArtifact = new MavenArtifact(pom.getModel(),
                new ArtifactCoordinates(pom.getGroupId(),
                        pom.getArtifactId(),
                        pom.getVersion()),
                pom.getArtifact().getFile());
        try {
            if (this.onlySpecs) {
                final File gemspec = this.converter.createGemspecFromArtifact(mavenArtifact,
                                                                              targetDirectory());
                getLog().info("created gemspec: " + gemspec);
                return gemspec;
            }
            else {
                final GemArtifact gemArtifact = this.converter.createGemFromArtifact(mavenArtifact,
                                                                                     targetDirectory());
                getLog().info("created gem: " + gemArtifact.getGemFile());
                return gemArtifact.getGemFile();
            }
        }
        catch (final IOException e) {
            throw new MojoExecutionException("error converting artifact " + pom,
                    e);
        }
    }

    private ProjectBuildingResult buildMavenProject(final Artifact artifact,
            final boolean resolveDependencies) throws MojoExecutionException {
        try {
            final ProjectBuildingRequest request = new DefaultProjectBuildingRequest();
            request.setLocalRepository(this.localRepository)
                    .setRemoteRepositories(this.remoteRepositories)
                    .setResolveDependencies(resolveDependencies)
                    .setRepositorySession(this.repositorySession)
                    .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL)
                    // FIXME hack to fix a problem with jdk determination on com.thoughtworks.xstream:xstream
                    .setActiveProfileIds(Arrays.asList("jdk16", "jdk14", "jdk15", "jdk17"));
            return this.builder.build(artifact, request);
        }
        catch (final ProjectBuildingException e) {
            throw new MojoExecutionException("error in building project for "
                    + artifact, e);
        }
    }

    private File targetDirectory() {
        if (this.tempDir == null) {
            return new File(this.project.getBuild()
                    .getDirectory()
                    .replaceFirst("[$][{]project.basedir[}].", ""));
        }
        else {
            return this.tempDir;
        }
    }
}
