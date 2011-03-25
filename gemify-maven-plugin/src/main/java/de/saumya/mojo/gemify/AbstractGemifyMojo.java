package de.saumya.mojo.gemify;

import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;

import de.saumya.mojo.ruby.gems.GemManager;

abstract public class AbstractGemifyMojo extends AbstractMojo {

    private static final List<ArtifactRepository> EMPTY_REPO_LIST = Collections.emptyList();

    /**
     * gemname to identify the maven artifact (format: groupId.artifactId).
     * 
     * @parameter default-value="${gemify.gemname}"
     * @required
     */
    String                  gemname;

    /** @parameter default-value="${gemify.repositories}" */
    private String                         repositories;

    /**
     * local repository for internal use.
     * 
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository    localRepository;

    /**
     * reference to maven project for internal use.
     * 
     * @parameter default-value="${project}"
     * @required
     * @readonly true
     */
    protected MavenProject          project;

    /** @component */
    protected RepositorySystem        repositorySystem;

    /**
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    protected RepositorySystemSession repositorySession;

    protected List<ArtifactRepository> remoteRepositories;
    
    public void execute() throws MojoExecutionException {
        if (this.gemname == null) {
            throw new MojoExecutionException("no gemname given, use '-Dgemify.gemname=...' to specify one");
        }
        // remove the mvn:prefix if any
        this.gemname = this.gemname.replaceFirst("^mvn:", "");
        if (!this.gemname.contains(GemManager.GROUP_ID_ARTIFACT_ID_SEPARATOR)) {
            throw new MojoExecutionException("not valid name for a maven-gem, it needs a at least one '" + GemManager.GROUP_ID_ARTIFACT_ID_SEPARATOR 
                                             + "'");
        }

        if(repositories != null){
            for(String repoUrl: this.repositories.split(",")){
                ArtifactRepository repository = this.repositorySystem.createArtifactRepository(repoUrl.replaceFirst("https?://", "").replaceAll("[:\\/&?=.]", "_"),
                                                                                               repoUrl,
                                                                                               new DefaultRepositoryLayout(),
                                                                                               new ArtifactRepositoryPolicy(),
                                                                                               new ArtifactRepositoryPolicy());
                this.project.getRemoteArtifactRepositories().add(repository);
            }
        }
        if(repositorySession.isOffline()){
            remoteRepositories = EMPTY_REPO_LIST;
        }
        else {
            remoteRepositories = this.project.getRemoteArtifactRepositories();
        }

        executeGemify();
    }

    abstract void executeGemify() throws MojoExecutionException;
}
