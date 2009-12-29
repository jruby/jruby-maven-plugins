package de.saumya.mojo.gem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.util.IOUtil;

import de.saumya.mojo.jruby.AbstractJRubyMojo;

/**
 * @goal initialize
 */
@SuppressWarnings("deprecation")
public class InitializeMojo extends AbstractJRubyMojo {

    /**
     * @component
     */
    ArtifactMetadataSource metadata;

    @Override
    public void execute() throws MojoExecutionException {
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
                    // final ArtifactResolutionResult arr =
                    this.resolver.resolveTransitively(artifacts,
                                                      artifact,
                                                      this.remoteRepositories,
                                                      this.localRepository,
                                                      this.metadata);

                    // System.out.println(artifact + " " + arr.getArtifacts());
                }
                catch (final InvalidDependencyVersionException e) {
                    throw new MojoExecutionException("error resolving artifacts for "
                            + artifact,
                            e);
                }
                catch (final ArtifactResolutionException e) {
                    throw new MojoExecutionException("error resolving artifacts for "
                            + artifact,
                            e);
                }
                catch (final ArtifactNotFoundException e) {
                    throw new MojoExecutionException("error resolving artifacts for "
                            + artifact,
                            e);
                }

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

}
