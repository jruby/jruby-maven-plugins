package de.saumya.mojo.gemify;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;

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
public class GemifyMojo extends AbstractMojo {

    private static final String SEPARATOR = "------------------------";

    /**
     * gemname to identify the maven artifact (format: groupId.artifactId).
     * 
     * @parameter default-value="${gemify.gemname}"
     * @required
     */
    private String                          gemName;

    /**
     * the version of the maven artifact which gets gemified.
     * 
     * @parameter default-value="${gemify.version}"
     */
    private String                          version;

    /**
     * do not follow relocation but use relocated pom for the original. i.e.
     * using the given gemname with the content of the relocated artifact. if
     * set to false it will just follow the relocation and produce a gem with
     * the "relocated" gemname. default: false
     * 
     * @parameter default-value="${gemify.force}"
     */
    private boolean                         force;

    /**
     * @parameter default-value="${gemify.tempDir}"
     */
    private File                            targetDirectory;

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

    /**
     * local repository for internal use.
     * 
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository              localRepository;

    /**
     * reference to maven project for internal use.
     * 
     * @parameter default-value="${project}"
     * @required
     * @readonly true
     */
    private MavenProject                    project;

    /**
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    private RepositorySystemSession         repoSession;

    /** @component */
    private ProjectBuilder                  builder;

    /** @component */
    private MavenArtifactConverter          converter;

    /** @component */
    private RepositorySystem                repositorySystem;

    /** @component */
    private GemManager                      gemManager;

    private final Map<String, MavenProject> relocations = new HashMap<String, MavenProject>();

    public void execute() throws MojoExecutionException {
        if (this.gemName == null) {
            throw new MojoExecutionException("no gemname given, use '-Dgemify.gemname=...' to specify one");
        }
        if (!this.gemName.contains(".")) {
            throw new MojoExecutionException("not valid name for a maven-gem, it needs a at least one '.'");
        }

        final ProjectBuildingResult result = buildProject(this.gemName,
                                                          this.version,
                                                          this.onlySpecs);

        // visit all dependencies and follow relocations if needed
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
                        getLog().info("skip optional " + artifact);
                    }
                }
            }
        }

        // gemify actual for given gemname
        gemifyMavenProject(result.getProject());

        // gemify orphaned dependencies as desired
        // TODO may be going through the visited map is sufficient ?
        if (!this.skipDependencies && !this.onlySpecs) {
            for (final Map.Entry<String, Node> entry : visited.entrySet()) {
                if (!entry.getValue().isOrphaned()
                        && entry.getValue().parent != null) {
                    gemifyMavenProject(entry.getValue().project);
                }
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
    private ProjectBuildingResult buildProject(final String gemName,
            final String version, final boolean onlyPom)
            throws MojoExecutionException {
        try {
            Artifact artifact = null;
            if (version == null) {
                artifact = this.gemManager.createJarArtifactForGemnameWithLatestVersion(this.gemName,
                                                                                        this.localRepository,
                                                                                        this.project.getRemoteArtifactRepositories());
            }
            else {
                // find the latest version
                final List<String> versions = this.gemManager.availableVersions(this.gemManager.createJarArtifactForGemname(this.gemName,
                                                                                                                            null),
                                                                                this.localRepository,
                                                                                this.project.getRemoteArtifactRepositories());

                // the given version is the gem version, so find the respective
                // maven-version
                final Maven2GemVersionConverter converter = new Maven2GemVersionConverter();
                for (final String v : versions) {
                    if (version.equals(converter.createGemVersion(v))) {
                        artifact = this.gemManager.createJarArtifactForGemname(this.gemName,
                                                                               v);
                        break;
                    }
                }
                // did not find it ? then assume the given version be already
                // maven-version
                if (artifact == null) {
                    artifact = this.gemManager.createJarArtifactForGemname(this.gemName,
                                                                           version);
                }
            }
            return buildProject(artifact, onlyPom);
        }
        catch (final GemException e) {
            throw new MojoExecutionException("Error creating artifact when gemifying: "
                    + this.gemName,
                    e);
        }
    }

    private ProjectBuildingResult buildProject(final String groupId,
            final String artifactId, final String version)
            throws MojoExecutionException {
        final Artifact artifact = this.gemManager.createArtifact(groupId,
                                                                 artifactId,
                                                                 version,
                                                                 "jar");

        return buildProject(artifact, false);
    }

    private ProjectBuildingResult buildProject(Artifact artifact,
            final boolean isPom) throws MojoExecutionException {
        Relocation relocation = null;
        Artifact original = null;
        ProjectBuildingResult result;
        do {
            if (relocation != null) {
                artifact = this.gemManager.createArtifact(relocation.getGroupId(),
                                                          relocation.getArtifactId(),
                                                          relocation.getVersion() == null
                                                                  ? artifact.getVersion()
                                                                  : relocation.getVersion(),
                                                          isPom ? "pom" : "jar");
            }
            result = buildMavenProject(artifact, !isPom);

            if (result.getProject().getDistributionManagement() != null) {
                relocation = result.getProject()
                        .getDistributionManagement()
                        .getRelocation();
                if (relocation != null) {
                    if (this.gemName != null) {
                        // warning only for the top level gem
                        getLog().info("\n\n\tartifact is relocated to "
                                + relocation.getGroupId()
                                + "."
                                + relocation.getArtifactId()
                                + " version="
                                + relocation.getVersion()
                                + (relocation.getMessage() == null ? "" : " "
                                        + relocation.getMessage())
                                + "\n\tif you need the original gem you can recreate it with '-Dgemify.force'\n\n");
                    }
                    if (original == null) {
                        // remember the original artifact to be used as gem
                        // coordinate
                        original = artifact;
                    }
                }
            }
            else {
                relocation = null;
            }
        }
        while (relocation != null);

        if (original != null) {
            if (this.force) {
                // use the original artifact coordinates to generate the gem
                result.getProject().setGroupId(original.getGroupId());
                result.getProject().setArtifactId(original.getArtifactId());
                result.getProject().setVersion(original.getVersion());
            }
            else {
                this.relocations.put(original.getGroupId() + ":"
                        + original.getArtifactId() + ":"
                        + original.getVersion(), result.getProject());
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

            MavenProject relocated = this.relocations.get(dep.getGroupId()
                    + ":" + dep.getArtifactId() + ":" + dep.getVersion());
            if (relocated != null) {
                getLog().debug("apply relocation from " + dep + " to "
                        + relocated.getArtifact());
                dep.setGroupId(relocated.getGroupId());
                dep.setArtifactId(relocated.getArtifactId());
                dep.setVersion(relocated.getVersion());
            }

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
                    relocated = this.relocations.get(dep.getGroupId() + ":"
                            + dep.getArtifactId() + ":" + dep.getVersion());
                    if (relocated != null) {
                        getLog().debug("apply relocation from " + dep + " to "
                                + relocated.getArtifact());
                        dep.setGroupId(relocated.getGroupId());
                        dep.setArtifactId(relocated.getArtifactId());
                        dep.setVersion(relocated.getVersion());
                    }

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
        getLog().debug("gemify " + pom);

        if (pom.getArtifact().getFile() == null
                || !pom.getArtifact().getFile().exists()) {
            final ArtifactResolutionRequest request = new ArtifactResolutionRequest();
            request.setArtifact(pom.getArtifact())
                    .setLocalRepository(this.localRepository)
                    .setRemoteRepositories(this.project.getRemoteArtifactRepositories())
                    .setResolveRoot(!this.onlySpecs)
                    .setResolveTransitively(false);
            final ArtifactResolutionResult result = this.repositorySystem.resolve(request);
            if (result.getMissingArtifacts().size() > 0) {
                // prepare the exception message
                StringBuilder buf = new StringBuilder();
                for(Artifact artifact: result.getMissingArtifacts()){
                    buf.append("\n\nMissing Artifacts:\n").append(SEPARATOR);
                    try {
                        MavenProject model = gemManager.buildModel(artifact, this.repoSession, localRepository,
                                                             this.project.getRemoteArtifactRepositories(), false);
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

                throw new MojoExecutionException((this.repoSession.isOffline()
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
                                                                              targetDirectoryFromProject());
                getLog().info("created gemspec: " + gemspec);
                return gemspec;
            }
            else {
                final GemArtifact gemArtifact = this.converter.createGemFromArtifact(mavenArtifact,
                                                                                     targetDirectoryFromProject());
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
                    .setRemoteRepositories(this.project.getRemoteArtifactRepositories())
                    .setResolveDependencies(resolveDependencies)
                    .setRepositorySession(this.repoSession)
                    .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
            return this.builder.build(artifact, request);
        }
        catch (final ProjectBuildingException e) {
            throw new MojoExecutionException("error in building project for "
                    + artifact, e);
        }
    }

    private File targetDirectoryFromProject() {
        if (this.targetDirectory == null) {
            return new File(this.project.getBuild()
                    .getDirectory()
                    .replaceFirst("[$][{]project.basedir[}].", ""));
        }
        else {
            return this.targetDirectory;
        }
    }
}
