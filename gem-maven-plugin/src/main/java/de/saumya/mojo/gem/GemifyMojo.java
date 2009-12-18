package de.saumya.mojo.gem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.util.StringUtils;

/**
 * goal to convert that artifact into a gem.
 * 
 * @goal gemify
 * @phase package
 */
public class GemifyMojo extends BuildMojo {
    // /**
    // * arguments for the gem command of JRuby.
    // *
    // * @parameter default-value="${jruby.gem.args}"
    // */
    // protected String args = null;

    /**
     * @parameter default-value="${project.build.directory}/gemify"
     */
    File                gemify;

    /**
     * @parameter default-value="${project.build.directory}"
     */
    File                buildDirectory;

    /**
     * @component
     */
    MavenProjectBuilder builder;

    private File        launchDir;

    @Override
    public void execute() throws MojoExecutionException {
        final StringBuilder gems = new StringBuilder();
        try {
            final String gem = build(this.mavenProject,
                                     this.mavenProject.getArtifact().getFile());
            gems.append(gem).append(' ');
        }
        catch (final IOException e) {
            throw new MojoExecutionException("error gemifing pom", e);
        }
        for (final Artifact artifact : this.artifacts) {
            // only jar-files get gemified !!!
            if ("jar".equals(artifact.getType()) && !artifact.hasClassifier()) {
                try {
                    final MavenProject project = this.builder.buildFromRepository(artifact,
                                                                                  this.remoteRepositories,
                                                                                  this.localRepository);
                    final String gem = build(project, artifact.getFile());
                    gems.append(gem).append(" ");
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
        this.launchDir = this.launchDirectory;
        execute("-S gem install -l " + gems);
    }

    @SuppressWarnings("unchecked")
    private String build(final MavenProject project, final File jarfile)
            throws MojoExecutionException, IOException {

        getLog().info("build gem for " + jarfile);
        // final Map<String, String> dependencies = new HashMap<String,
        // String>();
        // final Map<String, String> developmentDependencies = new
        // HashMap<String, String>();
        // depsFromArtifacts(dependencies, Artifact.SCOPE_COMPILE
        // + Artifact.SCOPE_RUNTIME);
        // depsFromArtifacts(developmentDependencies, Artifact.SCOPE_PROVIDED
        // + Artifact.SCOPE_TEST);

        final String gemName = project.getGroupId() + "."
                + project.getArtifactId();
        final File gemDir = new File(this.gemify, gemName);
        final File lib = new File(gemDir, "lib");
        lib.mkdirs();
        final File gemSpec = new File(gemDir, gemName + ".gemspec");
        final GemspecWriter gemSpecWriter = new GemspecWriter(gemSpec,
                project,
                false);

        gemSpecWriter.appendJarfile(jarfile, jarfile.getName());
        gemSpecWriter.appendPath(lib.getName());

        // for (final Artifact artifact : (Set<Artifact>)
        // project.getDependencyArtifacts()) {
        for (final Dependency artifact : (List<Dependency>) project.getDependencies()) {
            if ("jar".equals(artifact.getType())
                    && artifact.getClassifier() == null) {
                if ((Artifact.SCOPE_COMPILE + Artifact.SCOPE_RUNTIME).contains(artifact.getScope())) {
                    gemSpecWriter.appendDependency(artifact.getGroupId() + "."
                            + artifact.getArtifactId(), artifact.getVersion());
                }
                else if ((Artifact.SCOPE_PROVIDED + Artifact.SCOPE_TEST).contains(artifact.getScope())) {
                    gemSpecWriter.appendDevelopmentDependency(artifact.getGroupId()
                                                                      + "."
                                                                      + artifact.getArtifactId(),
                                                              artifact.getVersion());
                }
                else {
                    // TODO put things into "requirements"
                }
            }
        }

        gemSpecWriter.close();

        gemSpecWriter.copy(gemDir);

        FileWriter writer = null;
        try {
            writer = new FileWriter(new File(lib, project.getArtifactId()
                    + ".rb"));

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
        execute("-S gem build " + gemSpec.getAbsolutePath());

        return gemSpec.getAbsolutePath().replaceFirst(".gemspec$", "") + "-"
                + gemVersion(project.getVersion()) + "-java.gem";
    }

    private String titleizedClassname(final String artifactId) {
        final StringBuilder name = new StringBuilder();// artifact.getGroupId()).append(".");
        for (final String part : artifactId.split("-")) {
            name.append(StringUtils.capitalise(part));
        }
        return name.toString();
    }

    private String gemVersion(final String versionString) {
        final StringBuilder version = new StringBuilder();
        boolean first = true;
        for (final String part : versionString.replaceAll("beta", "1")
                .replaceAll("alpha", "0")
                .replaceAll("\\D+", ".")
                .split("\\.")) {
            if (part.length() > 0) {
                if (first) {
                    first = false;
                    version.append(part);
                }
                else {
                    version.append(".").append(part);
                }
            }
        }
        return version.toString();
    }

    @Override
    protected File launchDirectory() {
        return this.launchDir.getAbsoluteFile();
    }

}
