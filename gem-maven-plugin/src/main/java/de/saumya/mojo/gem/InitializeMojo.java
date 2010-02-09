package de.saumya.mojo.gem;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
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

    private static final int               ONE_DAY_IN_MILLIS = 86400000;
    private final List<ArtifactRepository> gemRepositories   = new ArrayList<ArtifactRepository>();

    @Override
    public void execute() throws MojoExecutionException {
        for (final ArtifactRepository repository : this.remoteRepositories) {
            // instanceof does not work probably a classloader issue !!!
            if (repository.getLayout()
                    .getClass()
                    .getName()
                    .equals(GemRepositoryLayout.class.getName())) {
                this.gemRepositories.add(repository);
            }
        }
        if (this.gemRepositories.size() == 0) {
            getLog().warn("gem plugin configured but no gem repository found");
            return;
        }
        final File gemsDir = new File(this.gemPath, "gems");
        final StringBuilder gems = new StringBuilder();
        final Map<String, Artifact> collectedArtifacts = new LinkedHashMap<String, Artifact>();
        collectArtifacts(this.project.getArtifact(), collectedArtifacts, true);
        collectedArtifacts.remove(key(this.project.getArtifact()));

        // System.out.println(collectedArtifacts.values());
        // System.out.println(this.project.getDependencyArtifacts());
        // System.out.println(this.project.getArtifacts());

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
                && (pom.lastModified() != artifact.getFile().lastModified() || pom.length() == artifact.getFile()
                        .length())) {
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
        getLog().info("collect artifact for " + artifact);
        if (artifact != this.project.getArtifact()) {
            // createMissingPom(artifact);

            resolve(artifact);
        }
        try {
            final MavenProject project = artifact != this.project.getArtifact()
                    ? this.builder.buildFromRepository(artifact,
                                                       this.remoteRepositories,
                                                       this.localRepository)
                    : this.project;

            createMetadata(project);
            // this.metadata.retrieve(artifact,
            // this.localRepository,
            // this.remoteRepositories);

            project.setDependencyArtifacts(project.createArtifacts(this.artifactFactory,
                                                                   artifact.getScope(),
                                                                   null));

            // final ArtifactResolutionRequest request = new
            // ArtifactResolutionRequest().setArtifact(project.getArtifact())
            // .setArtifactDependencies(project.getDependencyArtifacts())
            // .setLocalRepository(this.localRepository)
            // .setRemoteRepositories(project.getRemoteArtifactRepositories())
            // .setManagedVersionMap(project.getManagedVersionMap());
            // request.setResolveTransitively(true);
            final List<Artifact> filtered = new ArrayList<Artifact>();
            final ArtifactFilter filter = new ArtifactFilter() {

                @Override
                public boolean include(final Artifact artifact) {
                    if (Artifact.SCOPE_TEST.equals(artifact.getScope())) {
                        filtered.add(artifact);
                    }
                    // System.out.println(artifact
                    // + " "
                    // + artifact.getScope()
                    // + " "
                    // + (includeTest
                    // && Artifact.SCOPE_TEST.equals(artifact.getScope()) ||
                    // !Artifact.SCOPE_TEST.equals(artifact.getScope())));
                    return (includeTest && Artifact.SCOPE_TEST.equals(artifact.getScope()))
                            || !Artifact.SCOPE_TEST.equals(artifact.getScope());
                }
            };

            // System.out.println(artifact + " --> " + project.getArtifacts());
            if (artifact.getScope() == null
                    || (includeTest && Artifact.SCOPE_TEST.equals(artifact.getScope()))
                    || !Artifact.SCOPE_TEST.equals(artifact.getScope())) {
                final ArtifactResolutionResult result = this.resolver.resolveTransitively(project.getDependencyArtifacts(),
                                                                                          project.getArtifact(),
                                                                                          this.localRepository,
                                                                                          project.getRemoteArtifactRepositories(),
                                                                                          this.metadata,
                                                                                          filter);
                project.setArtifacts(result.getArtifacts());
                // System.out.println(artifact + " --> " +
                // project.getArtifacts());
                for (final Artifact depArtifact : (Set<Artifact>) project.getArtifacts()) {
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
                for (final Artifact depArtifact : (Set<Artifact>) project.getDependencyArtifacts()) {
                    if (depArtifact.getFile() != null
                            && depArtifact.getFile().exists()) {
                        createMissingPom(depArtifact);
                    }
                }
                for (final Artifact depArtifact : filtered) {
                    resolve(depArtifact);
                }

                getLog().info("visited " + artifact + " "
                        + result.getArtifacts()
                        + project.getDependencyArtifacts());
                visitedArtifacts.put(key(artifact), artifact);
            }
            else {
                getLog().info("skipped visit of " + artifact);
            }
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
        catch (final InvalidVersionSpecificationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @SuppressWarnings("unchecked")
    private void createMetadata(final MavenProject project)
            throws InvalidVersionSpecificationException, MojoExecutionException {
        getLog().info("process metadata for " + project.getArtifact() + " "
                + project.getDependencies());
        for (final Dependency dep : (List<Dependency>) project.getDependencies()) {
            if ("gem".equals(dep.getType())) {
                final Artifact dependencyArtifact = this.artifactFactory.createDependencyArtifact(dep.getGroupId(),
                                                                                                  dep.getArtifactId(),
                                                                                                  VersionRange.createFromVersionSpec(dep.getVersion()),
                                                                                                  dep.getType(),
                                                                                                  dep.getClassifier(),
                                                                                                  dep.getScope());
                final ArtifactRepositoryMetadata repositoryMetadata = new ArtifactRepositoryMetadata(dependencyArtifact);

                // TODO do not assume to have only ONE gem repository
                final File metadataFile = new File(this.localRepository.getBasedir(),
                        this.localRepository.pathOfLocalRepositoryMetadata(repositoryMetadata,
                                                                           this.gemRepositories.get(0)));

                // update them only once a day
                if (System.currentTimeMillis() - metadataFile.lastModified() > ONE_DAY_IN_MILLIS) {
                    getLog().info("creating metadata for " + dependencyArtifact);

                    metadataFile.getParentFile().mkdirs();
                    execute(new String[] {
                                    "-e",
                                    "ARGV[0] = '"
                                            + dependencyArtifact.getArtifactId()
                                            + "'\nrequire('"
                                            + embeddedRubyFile("metadata.rb")
                                            + "')" },
                            metadataFile);
                }
            }
        }
    }

    private void resolve(final Artifact artifact) throws MojoExecutionException {
        if (this.project.getArtifact() != artifact
                && artifact.getFile() == null || !artifact.getFile().exists()) {
            getLog().info("resolve " + artifact);

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
}
