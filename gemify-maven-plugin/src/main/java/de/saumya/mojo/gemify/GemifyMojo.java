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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ResolutionNode;
import org.apache.maven.model.Relocation;
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
    private String                    gemName;
    /**
     * the version of the maven artifact which gets gemified.
     * 
     * @parameter expression="${gemify.version}"
     * @required
     */
    private String                    version;

    /**
     * do not follow relocation but use relocated pom for the original
     * 
     * @parameter expression="${gemify.force}" default-value="false"
     */
    private boolean                   force;

    /**
     * local repository for internal use.
     * 
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository        localRepository;

    /**
     * reference to maven project for internal use.
     * 
     * @parameter expression="${project}"
     * @required
     * @readOnly true
     */
    private MavenProject              project;
    /**
     * @parameter default-value="${settings.offline}"
     * @required
     * @readonly
     */
    boolean                           offline;

    /** @component */
    private ProjectBuilder            builder;

    /** @component */
    private MavenArtifactConverter    converter;

    /** @component */
    private RepositoryMetadataManager repositoryMetadataManager;

    /** @component */
    private ArtifactHandlerManager    artifactHandlerManager;

    /** @component */
    private RepositorySystem          repositorySystem;

    public void execute() throws MojoExecutionException {
        if (this.gemName == null) {
            throw new MojoExecutionException("no gemname given, use '-Dgemify.gemname=...' to specify one");
        }
        if (!this.gemName.contains(".")) {
            throw new MojoExecutionException("not valid name for a maven-gem, it needs a at least one '.'");
        }

        final DefaultGemifyManager gemify = new DefaultGemifyManager();
        gemify.artifactHandlerManager = this.artifactHandlerManager;
        gemify.repositoryMetadataManager = this.repositoryMetadataManager;
        Relocation relocation = null;
        Artifact original = null;
        do {
            final Artifact artifact;
            if (relocation == null) {
                artifact = gemify.resolveArtifact(this.gemName,
                                                  this.version,
                                                  this.localRepository,
                                                  this.project.getRemoteArtifactRepositories());
            }
            else {
                artifact = gemify.resolveArtifact(relocation.getGroupId()
                                                          + "."
                                                          + relocation.getArtifactId(),
                                                  relocation.getVersion(),
                                                  this.localRepository,
                                                  this.project.getRemoteArtifactRepositories());
            }
            final ProjectBuildingResult result = buildMavenProject(artifact,
                                                                   true);

            if (result.getProject().getDistributionManagement() != null) {
                relocation = result.getProject()
                        .getDistributionManagement()
                        .getRelocation();
                if (relocation != null) {
                    getLog().info("\n\n\tartifact is relocated to "
                            + relocation.getGroupId()
                            + "."
                            + relocation.getArtifactId()
                            + " version="
                            + relocation.getVersion()
                            + (relocation.getMessage() == null ? "" : " "
                                    + relocation.getMessage())
                            + "\n\tif you need the original gem you can recreate it with '-Dgemify.force'\n\n");
                    if (original == null) {
                        original = artifact;
                    }
                    continue;
                }
            }
            else {
                relocation = null;
            }
            if (this.force && original != null) {
                result.getProject().setGroupId(original.getGroupId());
                result.getProject().setArtifactId(original.getArtifactId());
                result.getProject().setVersion(original.getVersion());
            }
            final Map<String, File> map = new HashMap<String, File>();
            File gem = gemifyMavenProject(result.getProject());
            map.put(artifactKey(result.getProject().getArtifact()), gem);
            for (final Artifact a : result.getArtifactResolutionResult()
                    .getArtifacts()) {
                gem = gemifyArtifact(a);
                map.put(artifactKey(a), gem);
            }

            final List<String> visited = new ArrayList<String>();

            visit(result.getArtifactResolutionResult()
                    .getArtifactResolutionNodes()
                    .iterator(), visited);
            visited.add(artifactKey(result.getProject().getArtifact()));

            for (final String key : visited) {
                getLog().error(map.get(key).getAbsolutePath());
            }
        }
        while (relocation != null);
    }

    private String artifactKey(final Artifact artifact) {
        return artifact.getGroupId() + "." + artifact.getArtifactId();
    }

    private void visit(final Iterator<ResolutionNode> iter,
            final List<String> visited) {
        while (iter.hasNext()) {
            final ResolutionNode n = iter.next();
            if (n.isResolved()) {
                final String key = artifactKey(n.getArtifact());
                getLog().error(n.getArtifact().toString() + " " + n.getDepth()
                        + " " + visited.contains(key));
                if (!visited.contains(key)) {
                    visit(n.getChildrenIterator(), visited);
                    visited.add(key);
                }
            }
        }
    }

    private File gemifyArtifact(final Artifact artifact)
            throws MojoExecutionException {
        final ProjectBuildingResult result = buildMavenProject(artifact, true);
        return gemifyMavenProject(result.getProject());
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
        getLog().error(pom.getArtifact().getFile().getAbsolutePath());
        try {
            final GemArtifact gemArtifact = this.converter.createGemFromArtifact(mavenArtifact,
                                                                                 targetDirectoryFromProject());
            getLog().info("gem: " + gemArtifact.getGemFile());
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
                    .setOffline(this.offline);
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
                .replaceFirst("${project.basedir}.", ""));
    }
}
