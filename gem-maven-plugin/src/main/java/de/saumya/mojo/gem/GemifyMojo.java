package de.saumya.mojo.gem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Relocation;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.util.StringUtils;

import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * goal to convert that artifact into a gem.
 */
@Mojo( name = "gemify", requiresDependencyResolution = ResolutionScope.TEST, requiresProject = true )
public class GemifyMojo extends AbstractGemMojo {

    @Parameter( property = "artifactId", defaultValue="${artifactId}" )
    String                            artifactId;

    @Parameter( property = "groupId", defaultValue="${groupId}" )
    String                            groupId;

    @Parameter( property = "version", defaultValue="${version}" )
    String                            version;

    @Parameter( defaultValue="${project.build.directory}/gemify" )
    File                              gemify;

    @Parameter( defaultValue="${project.build.directory}" )
    File                              buildDirectory;
    @Parameter( property = "skipGemInstall", defaultValue="${skipGemInstall}" )
    public boolean                    skipGemInstall = false;

    @Parameter( defaultValue="${repositorySystemSession}", readonly = true )
    private Object   repositorySession;

    @Component
    protected ProjectBuilder          builder;

    private final Map<String, String> relocationMap  = new HashMap<String, String>();

    @Override
    public void executeJRuby() throws MojoExecutionException, IOException,
            ScriptException {
        if (this.project.getBasedir() == null
                || !this.project.getBasedir().exists()) {
            if (!this.buildDirectory.exists()) {
                this.buildDirectory = new File("target");
            }
            if (!this.gemify.exists()) {
                this.gemify = new File(this.buildDirectory, "gemify");
            }
            // TODO this should be obsolete
//            if (!this.gemHome.exists()) {
//                this.gemHome = new File(this.buildDirectory, "rubygems");
//            }
//            if (!this.gemPath.exists()) {
//                this.gemPath = new File(this.buildDirectory, "rubygems");
//            }
        }
        if (this.artifactId != null || this.groupId != null
                || this.version != null) {
            if (this.artifactId != null && this.groupId != null
                    && this.version != null) {
                final Artifact artifact = this.repositorySystem.createArtifactWithClassifier(this.groupId,
                                                                                             this.artifactId,
                                                                                             this.version,
                                                                                             "jar",
                                                                                             null);
                ArtifactResolutionRequest request = new ArtifactResolutionRequest().setArtifact(artifact)
                        .setLocalRepository(this.localRepository)
                        .setRemoteRepositories(this.project.getRemoteArtifactRepositories());
                this.repositorySystem.resolve(request);
                try {
                    final MavenProject project = projectFromArtifact(artifact);
                    project.setArtifact(artifact);
                    final Set<Artifact> artifacts = new LinkedHashSet<Artifact>();
                    getLog().info("artifacts=" + artifacts);
                    request = new ArtifactResolutionRequest().setArtifact(artifact)
                            .setLocalRepository(this.localRepository)
                            .setRemoteRepositories(this.project.getRemoteArtifactRepositories())
                            .setManagedVersionMap(project.getManagedVersionMap());
                    final ArtifactResolutionResult arr = this.repositorySystem.resolve(request);
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
                catch (final GemException e) {
                    throw new MojoExecutionException("error building project object model",
                            e);
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
            throws MojoExecutionException, IOException, ScriptException {
        getLog().info("gemify( " + project + ", " + artifacts + " )");
        final Map<String, MavenProject> gems = new HashMap<String, MavenProject>();
        try {
            if(project.getArtifact().getFile() == null){
                throw new MojoExecutionException("no artifact file found: " + project.getArtifact() + "\n\trun 'package' goal first");
            }
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
                catch (final GemException e) {
                    getLog().error("skipping: " + artifact.getFile().getName(),
                                   e);
                }
            }
        }
        if (this.skipGemInstall) {
            getLog().info("skip installing gems");
        }
        else {
            // assume we have the dependent gems in place so tell gems to
            // install them without dependency check
            final Script script = this.factory.newScriptFromJRubyJar("gem")
                    .addArg("install")
                    .addArg("--backtrace")
                    .addArg((installRDoc ? "--" : "--no-") + "document")
                    .addArg("--ignore-dependencies")
                    .addArg("-l");
            for (final String gem : gems.keySet()) {
                script.addArg(gem);
            }
            script.executeIn(launchDirectory());
        }

    }

    private MavenProject projectFromArtifact(final Artifact artifact)
            throws ProjectBuildingException, GemException {

        final ProjectBuildingRequest request = new DefaultProjectBuildingRequest().setLocalRepository(this.localRepository)
                .setRemoteRepositories(this.project.getRemoteArtifactRepositories());
        
        manager.setRepositorySession(request, this.repositorySession );
        
        final MavenProject project = this.builder.build(artifact, request)
                .getProject();
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

    private String build(final MavenProject project, final File jarfile)
            throws MojoExecutionException, IOException, ScriptException {

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
                    arti = this.repositorySystem.createArtifactWithClassifier(dependency.getGroupId(),
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
                catch (final GemException e) {
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

        getLog().debug("<gemify> A");
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
        // this.launchDir = gemDir;
        getLog().debug("<gemify> B");
        this.factory.newScriptFromJRubyJar("gem")
                .addArg("build")
                .addArg("--backtrace")
                .addArg("--verbose")
                .addArg(gemSpec)
                .executeIn(gemDir);
        getLog().debug("<gemify> C");

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

    // @Override
    // protected File launchDirectory() {
    // if (this.launchDir != null) {
    // return this.launchDir.getAbsoluteFile();
    // }
    // else {
    // return super.launchDirectory();
    // }
    // }

    @Override
    protected void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException {
        // TODO Auto-generated method stub

    }

}
