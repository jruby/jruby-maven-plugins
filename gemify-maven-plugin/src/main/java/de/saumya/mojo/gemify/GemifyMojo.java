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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;

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
     * project builder for internal use.
     * 
     * @component
     */
    protected ProjectBuilder          builder;

    /**
     * local repository for internal use.
     * 
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository      localRepository;

    /**
     * reference to maven project for internal use.
     * 
     * @parameter expression="${project}"
     * @required
     * @readOnly true
     */
    protected MavenProject            project;

    /**
     * maven artifact converter for internal use.
     * 
     * @component
     */
    protected MavenArtifactConverter  converter;

    /**
     * for internal use.
     * 
     * @component
     */
    private RepositoryMetadataManager repositoryMetadataManager;

    /**
     * for internal use.
     * 
     * @component
     */
    protected ArtifactHandlerManager  artifactHandlerManager;

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

        final Artifact artifact = gemify.resolveArtifact(this.gemName,
                                                         this.version,
                                                         this.localRepository,
                                                         this.project.getRemoteArtifactRepositories());
        final ProjectBuildingResult result = buildMavenProject(artifact, true);
        gemifyMavenProject(result.getProject());
        for (final Artifact a : result.getArtifactResolutionResult()
                .getArtifacts()) {
            gemifyArtifact(a);
        }
    }

    private void gemifyArtifact(final Artifact artifact)
            throws MojoExecutionException {
        gemifyMavenProject(buildMavenProject(artifact, false).getProject());
    }

    private void gemifyMavenProject(final MavenProject pom)
            throws MojoExecutionException {
        getLog().debug("gemify " + pom);

        final MavenArtifact mavenArtifact = new MavenArtifact(pom.getModel(),
                new ArtifactCoordinates(pom.getGroupId(),
                        pom.getArtifactId(),
                        pom.getVersion()),
                pom.getArtifact().getFile());
        try {
            final GemArtifact gemArtifact = this.converter.createGemFromArtifact(mavenArtifact,
                                                                                 targetDirectoryFromProject());
            getLog().info("gem: " + gemArtifact.getGemFile());
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
            request.setLocalRepository(this.localRepository);
            request.setRemoteRepositories(this.project.getRemoteArtifactRepositories());
            request.setResolveDependencies(true);
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
                .replaceFirst("[$].project.basedir..", ""));
    }
}
