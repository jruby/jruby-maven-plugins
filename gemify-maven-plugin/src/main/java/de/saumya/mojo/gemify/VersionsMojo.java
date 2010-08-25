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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import de.saumya.mojo.ruby.GemException;
import de.saumya.mojo.ruby.GemifyManager;

/**
 * Goal which takes an maven artifact and converts it and its jar dependencies
 * to gem.
 * 
 * @goal versions
 * @requiresProject false
 */
public class VersionsMojo extends AbstractMojo {

    /**
     * gemname to identify the maven artifact (format: groupId.artifact).
     * 
     * @parameter expression="${gemify.gemname}"
     * @required
     */
    private String               gemName;

    /**
     * local repository for internal use.
     * 
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * reference to maven project for internal use.
     * 
     * @parameter expression="${project}"
     * @required
     * @readOnly true
     */
    protected MavenProject       project;

    /** @component */
    protected GemifyManager      gemify;

    public void execute() throws MojoExecutionException {
        if (this.gemName == null) {
            throw new MojoExecutionException("no gemname given, use '-Dgemify.gemname=...' to specify one");
        }
        if (!this.gemName.contains(".")) {
            throw new MojoExecutionException("not valid name for a maven-gem, it needs a at least one '.'");
        }

        // final DefaultGemifyManager gemify = new DefaultGemifyManager();
        // gemify.artifactHandlerManager = this.artifactHandlerManager;
        // gemify.repositoryMetadataManager = this.repositoryMetadataManager;

        try {
            getLog().info("\n\n\t"
                    + this.gemName
                    + " "
                    + this.gemify.availableVersions(this.gemName,
                                                    this.localRepository,
                                                    this.project.getRemoteArtifactRepositories())
                    + "\n\n");
        }
        catch (final GemException e) {
            throw new MojoExecutionException("error gemify: " + this.gemName, e);
        }
    }
}
