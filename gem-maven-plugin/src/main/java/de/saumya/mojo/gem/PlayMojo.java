package de.saumya.mojo.gem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.util.IOUtil;

import de.saumya.mojo.jruby.AbstractJRubyMojo;

/**
 * @goal play
 */
public class PlayMojo extends AbstractJRubyMojo {

    /**
     * @component
     */
    ArtifactDeployer       deployer;

    // /**
    // * @component
    // */
    // ArtifactInstaller installer;

    // /**
    // * @component role-hint="gem"
    // */
    // ArtifactRepositoryLayout layout;
    /**
     * @component
     */
    ArtifactMetadataSource metadata;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            getLog().error(this.project.getCompileClasspathElements()
                    .toString());
        }
        catch (final DependencyResolutionRequiredException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        final File gemsDir = new File(this.gemPath, "gems");
        final StringBuilder gems = new StringBuilder();
        for (final Artifact artifact : this.artifacts) {
            if (artifact.getType().contains("gem")) {
                final String prefix = artifact.getGroupId().equals("rubygems")
                        ? ""
                        : artifact.getGroupId() + ".";
                final File gemDir = new File(gemsDir,
                        prefix
                                + (artifact.getFile()
                                        .getName()
                                        .replaceAll(".gem$", "").replace("-SNAPSHOT",
                                                                         "")));
                try {
                    final Set<Artifact> artifacts = this.project.createArtifacts(this.artifactFactory,
                                                                                 null,
                                                                                 null);
                    final ArtifactResolutionResult arr = this.resolver.resolveTransitively(artifacts,
                                                                                           artifact,
                                                                                           this.remoteRepositories,
                                                                                           this.localRepository,
                                                                                           this.metadata);

                    // System.out.println(artifact + " " + arr.getArtifacts());
                }
                catch (final InvalidDependencyVersionException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                catch (final ArtifactResolutionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (final ArtifactNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                // final File gemDirJava = new File(gemDir.getPath() + "-java");
                // TODO force installing gems via command line
                if (!(this.fork && gemDir.exists())) {
                    gems.append(" ").append(artifact.getFile()
                            .getAbsolutePath());
                }
                else {
                    getLog().info("skip installing in local rubygem: "
                            + artifact);
                }
                final File pom = new File(artifact.getFile()
                        .getPath()
                        .replaceFirst(".gem$", ".pom"));
                if (artifact.getGroupId().equals("rubygems")
                        && pom.lastModified() < artifact.getFile()
                                .lastModified()) {
                    getLog().info("creating pom for " + artifact);
                    FileWriter writer = null;
                    try {
                        writer = new FileWriter(pom);
                        writer.append("<project>\n"
                                + "<modelVersion>4.0.0</modelVersion>\n"
                                + "<groupId>")
                                .append(artifact.getGroupId())
                                .append("</groupId>\n" + "<artifactId>")
                                .append(artifact.getArtifactId())
                                .append("</artifactId>\n"
                                        + "<packaging>gem</packaging>\n"
                                        + "<version>")
                                .append(artifact.getVersion())
                                .append("</version>\n" + "</project>");
                    }
                    catch (final IOException e) {
                        throw new MojoExecutionException("error writing out pom for "
                                + artifact,
                                e);
                    }
                    finally {
                        IOUtil.close(writer);
                    }
                }
            }
        }
        if (gems.length() > 0) {
            getLog().info(gems.toString());
            execute("-S gem install -l " + gems);
        }
        else {
            getLog().info("no gems found to install");
        }
    }

    public void executeOld() throws MojoExecutionException {
        final ArtifactRepository ar = new DefaultArtifactRepository("gem",
                "file:/tmp/repository",
                new GemRepositoryLayout());
        // this.layout);
        // try {
        // this.installer.install(this.mavenProject.getArtifact().getFile(),
        // this.mavenProject.getArtifact(),
        // this.localRepository);
        // }
        // catch (final ArtifactInstallationException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // } // to install
        try {
            getLog().error(this.project.getArtifact().toString());
            this.deployer.deploy(this.project.getArtifact().getFile(),
                                 this.project.getArtifact(),
                                 ar,
                                 this.localRepository);
        }
        catch (final ArtifactDeploymentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } // to deploy (includes the
        // install)

    }
}
