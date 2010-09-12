package de.saumya.mojo.gem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Relocation;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.util.StringUtils;

import de.saumya.mojo.ruby.RubyScriptException;

/**
 * goal to convert that artifact into a gem.
 * 
 * @goal gemify
 * @requiresDependencyResolution test
 */
public class GemifyMojo extends AbstractGemMojo {

    /**
     * @parameter default-value="${artifactId}"
     */
    String                            artifactId;

    /**
     * @parameter default-value="${groupId}"
     */
    String                            groupId;

    /**
     * @parameter default-value="${version}"
     */
    String                            version;

    /**
     * @parameter default-value="${project.build.directory}/gemify"
     */
    File                              gemify;

    /**
     * @parameter default-value="${project.build.directory}"
     */
    File                              buildDirectory;

    /**
     * @parameter default-value="${skipGemInstall}"
     */
    public boolean                    skipGemInstall = false;

    private File                      launchDir;

    private final Map<String, String> relocationMap  = new HashMap<String, String>();

    @Override
    public void executeJRuby() throws MojoExecutionException, IOException,
            RubyScriptException {
        if (this.project.getBasedir() == null
                || !this.project.getBasedir().exists()) {
            if (!this.buildDirectory.exists()) {
                this.buildDirectory = new File("target");
            }
            if (!this.gemify.exists()) {
                this.gemify = new File(this.buildDirectory, "gemify");
            }
            if (!this.gemHome.exists()) {
                this.gemHome = new File(this.buildDirectory, "rubygems");
            }
            if (!this.gemPath.exists()) {
                this.gemPath = new File(this.buildDirectory, "rubygems");
            }
        }
        if (this.artifactId != null || this.groupId != null
                || this.version != null) {
            if (this.artifactId != null && this.groupId != null
                    && this.version != null) {
                final Artifact artifact = this.artifactFactory.createArtifactWithClassifier(this.groupId,
                                                                                            this.artifactId,
                                                                                            this.version,
                                                                                            "jar",
                                                                                            null);
                try {
                    // final ArtifactResolutionRequest request = new
                    // ArtifactResolutionRequest();
                    // request.setArtifact(artifact);
                    // request.setLocalRepository(this.localRepository);
                    // request.setRemoteRepostories(this.remoteRepositories);
                    this.resolver.resolve(artifact,
                                          this.project.getRemoteArtifactRepositories(),
                                          this.localRepository);
                }
                catch (final ArtifactResolutionException e) {
                    throw new MojoExecutionException("can not resolve "
                            + artifact.toString());
                }
                catch (final ArtifactNotFoundException e) {
                    throw new MojoExecutionException("can not resolve "
                            + artifact.toString());
                }
                try {
                    final MavenProject project = projectFromArtifact(artifact);
                    project.setArtifact(artifact);
                    final Set<Artifact> artifacts = project.createArtifacts(this.artifactFactory,
                                                                            null,
                                                                            null);
                    getLog().info("artifacts=" + artifacts);
                    final ArtifactResolutionResult arr = this.resolver.resolveTransitively(artifacts,
                                                                                           artifact,
                                                                                           this.project.getManagedVersionMap(),
                                                                                           this.localRepository,
                                                                                           this.project.getRemoteArtifactRepositories(),
                                                                                           this.metadata);
                    gemify(project, arr.getArtifacts());
                }
                catch (final InvalidDependencyVersionException e) {
                    throw new MojoExecutionException("can not resolve "
                            + artifact.toString(), e);
                }
                catch (final ProjectBuildingException e) {
                    throw new MojoExecutionException("error building project object model",
                            e);
                }
                catch (final ArtifactResolutionException e) {
                    throw new MojoExecutionException("can not resolve "
                            + artifact.toString(), e);
                }
                catch (final ArtifactNotFoundException e) {
                    throw new MojoExecutionException("can not resolve "
                            + artifact.toString(), e);
                }
            }
            else {
                throw new MojoExecutionException("not all three artifactId, groupId and version are given");
            }
        }
        else {
            gemify(this.project, this.project.getArtifacts());
        }
    }

    private void gemify(MavenProject project, final Set<Artifact> artifacts)
            throws MojoExecutionException, IOException, RubyScriptException {
        getLog().info("gemify( " + project + ", " + artifacts + " )");
        final Map<String, MavenProject> gems = new HashMap<String, MavenProject>();
        try {
            final String gem = build(project, project.getArtifact().getFile());
            gems.put(gem, project);
        }
        catch (final IOException e) {
            throw new MojoExecutionException("error gemifing pom", e);
        }
        for (final Artifact artifact : artifacts) {
            // only jar-files get gemified !!!
            if ("jar".equals(artifact.getType()) && !artifact.hasClassifier()) {
                try {
                    project = projectFromArtifact(artifact);
                    final String gem = build(project, artifact.getFile());
                    gems.put(gem, project);
                }
                catch (final ProjectBuildingException e) {
                    getLog().error("skipping: " + artifact.getFile().getName(),
                                   e);
                }
                catch (final IOException e) {
                    getLog().error("skipping: " + artifact.getFile().getName(),
                                   e);
                }
            }
        }
        this.launchDir = this.launchDirectory();
        if (this.skipGemInstall) {
            getLog().info("skip installing gems");
        }
        else {
            this.factory.newScriptFromResource(GEM_RUBY_COMMAND)
                    .addArg("install")
                    .addArg("-l")
                    .addArgs(orderInResolvedManner(gems))
                    .execute();
        }

    }

    private MavenProject projectFromArtifact(final Artifact artifact)
            throws ProjectBuildingException {

        final MavenProject project = this.builder.buildFromRepository(artifact,
                                                                      this.project.getRemoteArtifactRepositories(),
                                                                      this.localRepository);
        // System.out.println("\n\n ------------> " + artifact + "\n\n");
        if (project.getDistributionManagement() != null
                && project.getDistributionManagement().getRelocation() != null) {
            final Relocation reloc = project.getDistributionManagement()
                    .getRelocation();
            final String key = artifact.getGroupId() + ":"
                    + artifact.getArtifactId() + ":" + artifact.getType() + ":"
                    + artifact.getVersion();
            artifact.setArtifactId(reloc.getArtifactId());
            artifact.setGroupId(reloc.getGroupId());
            if (reloc.getVersion() != null) {
                artifact.setVersion(reloc.getVersion());
            }
            this.relocationMap.put(key, artifact.getGroupId() + ":"
                    + artifact.getArtifactId() + ":" + artifact.getType() + ":"
                    + artifact.getVersion());
            return projectFromArtifact(artifact);
        }
        else {
            return project;
        }
    }

    @SuppressWarnings("unchecked")
    private String orderInResolvedManner(final Map<String, MavenProject> gems)
            throws MojoExecutionException {
        final List<String> result = new ArrayList<String>();
        final Set<String> resolved = new HashSet<String>();
        int done = -1;
        while (result.size() != gems.size() && done != result.size()) {
            done = result.size();
            // System.out.println("\n" + result.size() + " ++++++ " +
            // gems.size()
            // + " results " + result + " ++++resolved " + resolved
            // + "\n ++++gems " + gems.keySet() + "\n"
            // + this.relocationMap);
            for (final Map.Entry<String, MavenProject> gem : gems.entrySet()) {
                // System.out.println("\n\tgem "
                // + gem
                // + " "
                // + resolved.contains(gem.getValue()
                // .getArtifact()
                // .toString()));
                if (!resolved.contains(gem.getValue().getArtifact().toString())) {
                    if (gem.getValue().getDependencies().isEmpty()) {
                        // System.out.println("\tresolved " + gem.getKey());
                        result.add(gem.getKey());
                        addResolved(resolved, gem.getValue());
                    }
                    else {
                        // System.out.println("\ttry "
                        // + gem.getValue().getArtifact()
                        // + gem.getValue().getDependencies());
                        boolean isResolved = true;
                        for (final Dependency dependency : gem.getValue()
                                .getDependencies()) {
                            if (!dependency.isOptional()
                                    && (Artifact.SCOPE_COMPILE + Artifact.SCOPE_RUNTIME).contains(dependency.getScope())) {
                                final String id = dependency.getGroupId() + ":"
                                        + dependency.getArtifactId() + ":"
                                        + dependency.getType() + ":"
                                        + dependency.getVersion();
                                // System.out.println(id);
                                if (!resolved.contains(id)) {

                                    final Artifact artifact = this.artifactFactory.createArtifactWithClassifier(dependency.getGroupId(),
                                                                                                                dependency.getArtifactId(),
                                                                                                                dependency.getVersion(),
                                                                                                                dependency.getType(),
                                                                                                                dependency.getClassifier());
                                    try {
                                        getLog().info("resolving: " + artifact);
                                        this.resolver.resolve(artifact,
                                                              this.project.getRemoteArtifactRepositories(),
                                                              this.localRepository);
                                    }
                                    catch (final ArtifactResolutionException e) {
                                        throw new MojoExecutionException("can not resolve "
                                                + artifact.toString());
                                    }
                                    catch (final ArtifactNotFoundException e) {
                                        throw new MojoExecutionException("can not resolve "
                                                + artifact.toString());
                                    }
                                    try {
                                        projectFromArtifact(artifact);
                                    }
                                    catch (final ProjectBuildingException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                    // System.out.println(this.relocationMap);
                                    isResolved = false;
                                    break;
                                }
                            }
                        }
                        if (isResolved) {
                            // System.out.println("\tresolved (with deps) "
                            // + gem.getKey());
                            result.add(gem.getKey());
                            addResolved(resolved, gem.getValue());
                        }
                    }
                }
            }
        }
        // System.out.println("----" + result);
        final StringBuilder str = new StringBuilder();
        boolean first = true;
        for (final String gem : result) {
            if (first) {
                first = false;
                str.append(gem);
            }
            else {
                str.append(' ').append(gem);
            }
        }
        return str.toString();
    }

    private void addResolved(final Set<String> resolved,
            final MavenProject project) {
        resolved.add(project.getArtifact().toString());
        if (project.getDistributionManagement() != null
                && project.getDistributionManagement().getRelocation() != null) {
            final Relocation dependency = project.getDistributionManagement()
                    .getRelocation();
            resolved.add(dependency.getGroupId() + ":"
                    + dependency.getArtifactId() + ":"
                    + project.getArtifact().getType() + ":"
                    + dependency.getVersion());
        }
    }

    @SuppressWarnings("unchecked")
    private String build(final MavenProject project, final File jarfile)
            throws MojoExecutionException, IOException, RubyScriptException {

        getLog().info("building gem for " + jarfile + " . . .");
        final String gemName = project.getGroupId() + "."
                + project.getArtifactId();
        final File gemDir = new File(this.gemify, gemName);
        final File gemSpec = new File(gemDir, gemName + ".gemspec");
        final GemspecWriter gemSpecWriter = new GemspecWriter(gemSpec,
                project,
                new GemArtifact(project));

        gemSpecWriter.appendJarfile(jarfile, jarfile.getName());
        final File lib = new File(gemDir, "lib");
        lib.mkdirs();
        // need relative filename here
        final File rubyFile = new File(lib.getName(), project.getGroupId()
                + "." + project.getArtifactId() + ".rb");
        gemSpecWriter.appendFile(rubyFile);

        for (final Dependency dependency : project.getDependencies()) {
            if (!dependency.isOptional() && "jar".equals(dependency.getType())
                    && dependency.getClassifier() == null) {
                getLog().info("--");
                getLog().info("dependency=" + dependency);
                // it will adjust the artifact as well (in case of relocation)
                Artifact arti = null;
                try {
                    arti = this.artifactFactory.createArtifactWithClassifier(dependency.getGroupId(),
                                                                             dependency.getArtifactId(),
                                                                             dependency.getVersion(),
                                                                             dependency.getScope(),
                                                                             dependency.getClassifier());
                    getLog().info("arti=" + arti);
                    projectFromArtifact(arti);
                    dependency.setGroupId(arti.getGroupId());
                    dependency.setArtifactId(arti.getArtifactId());
                    dependency.setVersion(arti.getVersion());
                }
                catch (final ProjectBuildingException e) {
                    throw new MojoExecutionException("error building project for "
                            + arti,
                            e);
                }

                if ((Artifact.SCOPE_COMPILE + Artifact.SCOPE_RUNTIME).contains(dependency.getScope())) {
                    gemSpecWriter.appendDependency(dependency.getGroupId()
                                                           + "."
                                                           + dependency.getArtifactId(),
                                                   dependency.getVersion());
                }
                else if ((Artifact.SCOPE_PROVIDED + Artifact.SCOPE_TEST).contains(dependency.getScope())) {
                    gemSpecWriter.appendDevelopmentDependency(dependency.getGroupId()
                                                                      + "."
                                                                      + dependency.getArtifactId(),
                                                              dependency.getVersion());
                }
                else {
                    // TODO put things into "requirements"
                }
            }
        }

        getLog().info("<gemify> A");
        gemSpecWriter.close();

        gemSpecWriter.copy(gemDir);

        FileWriter writer = null;
        try {
            // need absolute filename here
            writer = new FileWriter(new File(lib, rubyFile.getName()));

            writer.append("module ")
                    .append(titleizedClassname(project.getArtifactId()))
                    .append("\n");
            writer.append("  VERSION = '")
                    .append(gemVersion(project.getVersion()))
                    .append("'\n");
            writer.append("  MAVEN_VERSION = '")
                    .append(project.getVersion())
                    .append("'\n");
            writer.append("end\n");
            writer.append("begin\n");
            writer.append("  require 'java'\n");
            writer.append("  require File.dirname(__FILE__) + '/")
                    .append(jarfile.getName())
                    .append("'\n");
            writer.append("rescue LoadError\n");
            writer.append("  puts 'JAR-based gems require JRuby to load. Please visit www.jruby.org.'\n");
            writer.append("  raise\n");
            writer.append("end\n");
        }
        catch (final IOException e) {
            throw new MojoExecutionException("error writing ruby file", e);
        }
        finally {
            if (writer != null) {
                try {
                    writer.close();
                }
                catch (final IOException ignore) {
                }
            }
        }
        this.launchDir = gemDir;
        getLog().info("<gemify> B");
        this.factory.newScriptFromResource(GEM_RUBY_COMMAND)
                .addArg("build")
                .addArg(gemSpec)
                .executeIn(gemDir);
        getLog().info("<gemify> C");

        return gemSpec.getAbsolutePath().replaceFirst(".gemspec$", "") + "-"
                + gemVersion(project.getVersion()) + ".gem";
    }

    private String titleizedClassname(final String artifactId) {
        final StringBuilder name = new StringBuilder();// artifact.getGroupId()).append(".");
        for (final String part : artifactId.split("-")) {
            name.append(StringUtils.capitalise(part));
        }
        return name.toString();
    }

    private String gemVersion(final String versionString) {
        // needs to match with GemWriter#gemVersion
        return versionString.replaceAll("-SNAPSHOT", "")
                .replace("-", ".")
                .toLowerCase();
    }

    @Override
    protected File launchDirectory() {
        if (this.launchDir != null) {
            return this.launchDir.getAbsoluteFile();
        }
        else {
            return super.launchDirectory();
        }
    }

    @Override
    protected void executeWithGems() throws MojoExecutionException,
            RubyScriptException, IOException {
        // TODO Auto-generated method stub

    }

}
