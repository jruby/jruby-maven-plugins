package de.saumya.mojo.gemify;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
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

import de.saumya.mojo.gems.ArtifactCoordinates;
import de.saumya.mojo.gems.GemArtifact;
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

    /**
     * gemname to identify the maven artifact (format: groupId.artifact).
     * 
     * @parameter expression="${gemify.gemname}"
     * @required
     */
    private String                          gemName;
    /**
     * the version of the maven artifact which gets gemified.
     * 
     * @parameter expression="${gemify.version}"
     */
    private String                          version;

    /**
     * do not follow relocation but use relocated pom for the original
     * 
     * @parameter expression="${gemify.force}" default-value="false"
     */
    private boolean                         force;

    /**
     * gemify development depencendies as well.
     * 
     * @parameter expression="${gemify.development}" default-value="false"
     */
    private boolean                         development;

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
     * @parameter expression="${project}"
     * @required
     * @readOnly true
     */
    private MavenProject                    project;

    /**
     * @parameter default-value="${settings.offline}"
     * @required
     * @readonly
     */
    boolean                                 offline;

    /** @component */
    private ProjectBuilder                  builder;

    /** @component */
    private MavenArtifactConverter          converter;

    /** @component */
    private RepositorySystem                repositorySystem;

    /** @component */
    private GemManager                      gemify;

    private final Map<String, MavenProject> relocations = new HashMap<String, MavenProject>();

    public void execute() throws MojoExecutionException {
        if (this.gemName == null) {
            throw new MojoExecutionException("no gemname given, use '-Dgemify.gemname=...' to specify one");
        }
        if (!this.gemName.contains(".")) {
            throw new MojoExecutionException("not valid name for a maven-gem, it needs a at least one '.'");
        }

        // this.gemify = new DefaultGemifyManager();
        // this.gemify.artifactHandlerManager = this.artifactHandlerManager;
        // this.gemify.repositoryMetadataManager =
        // this.repositoryMetadataManager;

        final Map<String, Node> visited = new HashMap<String, Node>();

        final ProjectBuildingResult result = buildProject(this.gemName,
                                                          null,
                                                          null,
                                                          this.version);
        visit(visited,
              result,
              new Node(keyOf(result.getProject().getArtifact()),
                      result.getProject()));

        for (final Artifact artifact : result.getArtifactResolutionResult()
                .getArtifacts()) {
            if (inScope(artifact.getScope(), true)) {
                final Node node = visited.get(keyOf(artifact));
                if (node != null) {
                    gemifyMavenProject(visited.get(keyOf(artifact)).project);
                    visited.remove(keyOf(artifact));
                }
                else {
                    getLog().info("skip " + artifact);
                }
            }
        }
        gemifyMavenProject(result.getProject());
        for (final Map.Entry<String, Node> entry : visited.entrySet()) {
            if (!entry.getValue().isOrphaned()
                    && entry.getValue().parent != null) {
                gemifyMavenProject(entry.getValue().project);
            }

        }
    }

    /**
     * 
     * @param gemName
     *            if given then: groupId == null and artifactId == null
     * @param groupId
     *            if given then: gemName == null and artifactId != null
     * @param artifactId
     *            if given then: gemName == null and groupId != null
     * @param version
     * @return
     * @throws MojoExecutionException
     */
    private ProjectBuildingResult buildProject(final String gemName,
            final String groupId, final String artifactId, final String version)
            throws MojoExecutionException {
        Relocation relocation = null;
        Artifact original = null;
        try {
            ProjectBuildingResult result;
            do {
                Artifact artifact;
                if (relocation == null) {
                    if (gemName != null) {
                        artifact = this.gemify.createJarArtifactForGemname(gemName,
                                                                           version);
                        // this.localRepository,
                        // this.project.getRemoteArtifactRepositories());
                    }
                    else {
                        artifact = this.gemify.createArtifact(groupId,
                                                              artifactId,
                                                              version,
                                                              "jar");
                        // this.localRepository,
                        // this.project.getRemoteArtifactRepositories());
                    }
                }
                else {
                    artifact = this.gemify.createArtifact(relocation.getGroupId(),
                                                          relocation.getArtifactId(),
                                                          relocation.getVersion() == null
                                                                  ? version
                                                                  : relocation.getVersion(),
                                                          "jar");
                    // this.localRepository,
                    // this.project.getRemoteArtifactRepositories());
                }
                result = buildMavenProject(artifact, true);

                if (result.getProject().getDistributionManagement() != null) {
                    relocation = result.getProject()
                            .getDistributionManagement()
                            .getRelocation();
                    if (relocation != null) {
                        if (gemName != null) {
                            // warning only for the top level gem
                            getLog().info("\n\n\tartifact is relocated to "
                                    + relocation.getGroupId()
                                    + "."
                                    + relocation.getArtifactId()
                                    + " version="
                                    + relocation.getVersion()
                                    + (relocation.getMessage() == null
                                            ? ""
                                            : " " + relocation.getMessage())
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
        catch (final GemException e) {
            throw new MojoExecutionException("Error creating artifact when gemifying: "
                    + gemName,
                    e);
        }
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
                    final ProjectBuildingResult buildChild = buildProject(null,
                                                                          dep.getGroupId(),
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
                    .setResolveRoot(true)
                    .setResolveTransitively(false)
                    // follow the offline settings
                    .setForceUpdate(!this.offline)
                    .setOffline(this.offline);
            this.repositorySystem.resolve(request);
        }
        final MavenArtifact mavenArtifact = new MavenArtifact(pom.getModel(),
                new ArtifactCoordinates(pom.getGroupId(),
                        pom.getArtifactId(),
                        pom.getVersion()),
                pom.getArtifact().getFile());
        try {
            final GemArtifact gemArtifact = this.converter.createGemFromArtifact(mavenArtifact,
                                                                                 targetDirectoryFromProject());
            getLog().info("created gem: " + gemArtifact.getGemFile());
            return gemArtifact.getGemFile();
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
                    // follow the offline settings
                    .setForceUpdate(!this.offline)
                    .setOffline(this.offline)
                    .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
            return this.builder.build(artifact, request);
        }
        catch (final ProjectBuildingException e) {
            throw new MojoExecutionException("error in building project for "
                    + artifact, e);
        }
    }

    private File targetDirectoryFromProject() {
        return new File(this.project.getBuild()
                .getDirectory()
                .replaceFirst("[$][{]project.basedir[}].", ""));
    }
}
