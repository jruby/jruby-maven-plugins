package de.saumya.mojo.gem;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

import de.saumya.mojo.jruby.AbstractJRubyMojo;

/**
 * @goal initialize
 * @phase initialize
 */
public class InitializeMojo extends AbstractJRubyMojo {

    @Override
    public void execute() throws MojoExecutionException {
        final File gemsDir = new File(this.gemPath, "gems");
        final StringBuilder gems = new StringBuilder();
        final Map<String, Artifact> collectedArtifacts = new LinkedHashMap<String, Artifact>();
        collectArtifacts(this.project.getArtifact(), collectedArtifacts, true);
        collectedArtifacts.remove(key(this.project.getArtifact()));
        for (final Artifact artifact : collectedArtifacts.values()) {
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
                // TODO force flag to install gems via command line argument
                if (!(this.fork && gemDir.exists())) {
                    gems.append(" ").append(artifact.getFile()
                            .getAbsolutePath());
                }
                else {
                    getLog().info("already installed: " + artifact);
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

    private void createMissingPom(final Artifact artifact)
            throws MojoExecutionException {
        final File pom = new File(artifact.getFile()
                .getPath()
                .replaceFirst("(-java)?.gem$", ".pom"));
        if (artifact.getGroupId().equals("rubygems")
                && pom.lastModified() != artifact.getFile().lastModified()) {
            getLog().info("creating pom for " + artifact);

            execute(new String[] {
                    "-e",
                    "ARGV[0] = '" + artifact.getFile().getAbsolutePath()
                            + "'\nrequire('" + embeddedRubyFile("spec2pom.rb")
                            + "')" }, pom);
            pom.setLastModified(artifact.getFile().lastModified());
        }
    }

    private String key(final Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    @SuppressWarnings("unchecked")
    private void collectArtifacts(final Artifact artifact,
            final Map<String, Artifact> visitedArtifacts,
            final boolean includeTest) throws MojoExecutionException {
        if (artifact != this.project.getArtifact()) {
            if (artifact.getFile() == null || !artifact.getFile().exists()) {
                getLog().info("resolve " + artifact + " " + artifact.getType());

                // final ArtifactResolutionRequest request = new
                // ArtifactResolutionRequest().setArtifact(artifact)
                // .setLocalRepository(this.localRepository)
                // .setRemoteRepositories(this.project.getRemoteArtifactRepositories());

                try {
                    this.resolver.resolve(artifact,
                                          this.project.getRemoteArtifactRepositories(),
                                          this.localRepository);
                }
                catch (final ArtifactResolutionException e) {
                    throw new MojoExecutionException("resolve error", e);
                }
                catch (final ArtifactNotFoundException e) {
                    throw new MojoExecutionException("resolve error", e);
                }
            }
            createMissingPom(artifact);
        }
        try {
            final MavenProject project = artifact != this.project.getArtifact()
                    ? this.builder.buildFromRepository(artifact,
                                                       this.remoteRepositories,
                                                       this.localRepository)
                    : this.project;

            project.setDependencyArtifacts(project.createArtifacts(this.artifactFactory,
                                                                   null,
                                                                   null));

            // final ArtifactResolutionRequest request = new
            // ArtifactResolutionRequest().setArtifact(project.getArtifact())
            // .setArtifactDependencies(project.getDependencyArtifacts())
            // .setLocalRepository(this.localRepository)
            // .setRemoteRepositories(project.getRemoteArtifactRepositories())
            // .setManagedVersionMap(project.getManagedVersionMap());
            // request.setResolveTransitively(true);

            final ArtifactResolutionResult result = this.resolver.resolveTransitively(project.getDependencyArtifacts(),
                                                                                      project.getArtifact(),
                                                                                      project.getRemoteArtifactRepositories(),
                                                                                      this.localRepository,
                                                                                      this.metadata);

            project.setArtifacts(result.getArtifacts());
            for (final Artifact depArtifact : (Set<Artifact>) project.getDependencyArtifacts()) {
                if ((Artifact.SCOPE_COMPILE + "+" + Artifact.SCOPE_RUNTIME).contains(depArtifact.getScope())
                        && !visitedArtifacts.containsKey(key(depArtifact))) {
                    collectArtifacts(depArtifact, visitedArtifacts, false);
                }
                else {
                    if (depArtifact.getFile() != null
                            && depArtifact.getFile().exists()) {
                        createMissingPom(depArtifact);
                    }
                }
            }
            getLog().debug("visited " + artifact + " " + artifact.getType()
                    + result.getArtifacts());
            visitedArtifacts.put(key(artifact), artifact);
        }
        catch (final ProjectBuildingException e) {
            throw new MojoExecutionException("Unable to build project due to an invalid dependency version: "
                    + e.getMessage(),
                    e);
        }
        catch (final ArtifactResolutionException e) {
            throw new MojoExecutionException("resolve error", e);
        }
        catch (final ArtifactNotFoundException e) {
            throw new MojoExecutionException("resolve error", e);
        }
        catch (final InvalidDependencyVersionException e) {
            throw new MojoExecutionException("resolve error", e);
        }

    }
}
