package de.saumya.mojo.gem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Relocation;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import de.saumya.mojo.jruby.AbstractJRubyMojo;

/**
 * goal to convert that artifact into a gem.
 * 
 * @goal package
 * @requiresDependencyResolution test
 */
public class PackageMojo extends AbstractJRubyMojo {

	/**
	 * @parameter expression="${project.build.directory}"
	 */
	File buildDirectory;

	/**
	 * @parameter default-value="${gemspec}"
	 */
	File gemSpec;

	private File launchDir;

	private final Map<String, String> relocationMap = new HashMap<String, String>();

	/**
	 * @parameter expression="false"
	 */
	boolean includeDependencies;

	public void execute() throws MojoExecutionException {
		final MavenProject project = this.project;
		final GemArtifact artifact = new GemArtifact(project);
		try {
			if (this.gemSpec != null) {
				this.launchDir = this.project.getBasedir();
				if (this.launchDir == null) {
					this.launchDir = new File(System.getProperty("user.dir"));
				}

				execute("-S gem build " + this.gemSpec.getAbsolutePath(), false);

				final File gem = new File(this.launchDir, artifact.getGemFile());
				if (project.getFile() != null) {
					// only when the pom exist there will be an artifact
					FileUtils.copyFile(gem, artifact.getFile());
					gem.deleteOnExit();
				}
			} else {
				build(project, artifact);
			}
		} catch (final IOException e) {
			throw new MojoExecutionException("error gemifing pom", e);
		}
	}

	private MavenProject projectFromArtifact(final Artifact artifact) throws ProjectBuildingException {

		final MavenProject project = this.builder.buildFromRepository(artifact, this.remoteRepositories, this.localRepository);
		if (project.getDistributionManagement() != null && project.getDistributionManagement().getRelocation() != null) {
			final Relocation reloc = project.getDistributionManagement().getRelocation();
			final String key = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType() + ":" + artifact.getVersion();
			artifact.setArtifactId(reloc.getArtifactId());
			artifact.setGroupId(reloc.getGroupId());
			if (reloc.getVersion() != null) {
				artifact.setVersion(reloc.getVersion());
			}
			this.relocationMap.put(key, artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType() + ":" + artifact.getVersion());
			return projectFromArtifact(artifact);
		} else {
			return project;
		}
	}

	@SuppressWarnings( { "unchecked" })
	private void build(final MavenProject project, final GemArtifact artifact) throws MojoExecutionException, IOException {

		getLog().info("building gem for " + artifact + " . . ." + artifact.hasJarFile());
		getLog().info("include dependencies? " + this.includeDependencies);
		final File gemDir = new File(this.buildDirectory, artifact.getGemName());
		final File gemSpec = new File(gemDir, artifact.getGemName() + ".gemspec");
		final GemspecWriter gemSpecWriter = new GemspecWriter(gemSpec, project, artifact);

		final File rubyFile;
		if (artifact.hasJarFile()) {
			gemSpecWriter.appendJarfile(artifact.getJarFile(), artifact.getJarFile().getName());
			final File lib = new File(gemDir, "lib");
			lib.mkdirs();
			// need relative filename here
			rubyFile = new File(lib.getName(), artifact.getGemName() + ".rb");
			gemSpecWriter.appendFile(rubyFile);
		} else {
			rubyFile = null;
		}

		ArtifactResolutionResult jarDependencyArtifacts = null;
		if (this.includeDependencies) {
			try {
				jarDependencyArtifacts = this.resolver.resolveTransitively(project.getDependencyArtifacts(), project.getArtifact(), this.project.getManagedVersionMap(),
						this.localRepository, this.remoteRepositories, this.metadata, new ArtifactFilter() {
							public boolean include(Artifact candidate) {
								if (candidate == artifact) {
									return true;
								}
								boolean result = (candidate.getType().equals("jar") && ("compile".equals(candidate.getScope()) || "runtime".equals(candidate.getScope())));
								return result;
							}

						});
				for (Iterator each = jarDependencyArtifacts.getArtifacts().iterator(); each.hasNext();) {
					Artifact dependency = (Artifact) each.next();
					getLog().info(" -- include -- " + dependency);
					gemSpecWriter.appendJarfile(dependency.getFile(), dependency.getFile().getName());
				}
			} catch (ArtifactResolutionException e) {
				e.printStackTrace();
			} catch (ArtifactNotFoundException e) {
				e.printStackTrace();
			}
		}

		// TODO make it the maven way (src/main/ruby + src/test/ruby) or the
		// ruby way (lib + spec + test)
		final File libDir = new File(project.getBasedir(), "lib");
		final File specDir = new File(project.getBasedir(), "spec");
		final File testDir = new File(project.getBasedir(), "test");

		if (libDir.exists()) {
			gemSpecWriter.appendPath("lib");
		}
		if (specDir.exists()) {
			gemSpecWriter.appendPath("spec");
		}
		if (testDir.exists()) {
			gemSpecWriter.appendPath("test");
		}

		for (final Dependency dependency : (List<Dependency>) project.getDependencies()) {
			if (!dependency.isOptional() && dependency.getType().contains("gem")) {
				// it will adjust the artifact as well (in case of relocation)
				Artifact arti = null;
				try {
					arti = this.artifactFactory.createArtifactWithClassifier(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getScope(),
							dependency.getClassifier());
					projectFromArtifact(arti);
					dependency.setGroupId(arti.getGroupId());
					dependency.setArtifactId(arti.getArtifactId());
					dependency.setVersion(arti.getVersion());
				} catch (final ProjectBuildingException e) {
					throw new MojoExecutionException("error building project for " + arti, e);
				}

				final String prefix = dependency.getGroupId().equals("rubygems") ? "" : dependency.getGroupId() + ".";
				if ((Artifact.SCOPE_COMPILE + Artifact.SCOPE_RUNTIME).contains(dependency.getScope())) {
					gemSpecWriter.appendDependency(prefix + dependency.getArtifactId(), dependency.getVersion());
				} else if ((Artifact.SCOPE_PROVIDED + Artifact.SCOPE_TEST).contains(dependency.getScope())) {
					gemSpecWriter.appendDevelopmentDependency(prefix + dependency.getArtifactId(), dependency.getVersion());
				} else {
					// TODO put things into "requirements"
				}
			}
		}

		gemSpecWriter.close();

		gemSpecWriter.copy(gemDir);

		if (artifact.hasJarFile() && !rubyFile.exists()) {
			FileWriter writer = null;
			try {
				// need absolute filename here
				writer = new FileWriter(new File(gemDir, rubyFile.getPath()));

				writer.append("module ").append(titleizedClassname(project.getArtifactId())).append("\n");
				writer.append("  VERSION = '").append(artifact.getGemVersion()).append("'\n");
				writer.append("  MAVEN_VERSION = '").append(project.getVersion()).append("'\n");
				writer.append("end\n");
				writer.append("begin\n");
				writer.append("  require 'java'\n");
				writer.append("  require File.dirname(__FILE__) + '/").append(artifact.getJarFile().getName()).append("'\n");
				if (jarDependencyArtifacts != null) {
					for (Iterator each = jarDependencyArtifacts.getArtifacts().iterator(); each.hasNext();) {
						Artifact dependency = (Artifact) each.next();
						writer.append("  require File.dirname(__FILE__) + '/").append(dependency.getFile().getName()).append("'\n");
					}

				}
				writer.append("rescue LoadError\n");
				writer.append("  puts 'JAR-based gems require JRuby to load. Please visit www.jruby.org.'\n");
				writer.append("  raise\n");
				writer.append("end\n");
			} catch (final IOException e) {
				throw new MojoExecutionException("error writing ruby file", e);
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (final IOException ignore) {
					}
				}
			}
		}
		this.launchDir = gemDir;
		execute("-S gem build " + gemSpec.getAbsolutePath());

		// TODO share this with GemArtifactRepositoryLayout
		final StringBuilder gemFilename = new StringBuilder("rubygems".equals(artifact.getGroupId()) ? "" : artifact.getGroupId() + ".").append(artifact.getArtifactId()).append(
				"-").append(artifact.getGemVersion()).append(artifact.getClassifier() == null ? "" : "-java").append(".gem");

		FileUtils.copyFile(new File(gemDir, gemFilename.toString()), artifact.getFile());
	}

	private String titleizedClassname(final String artifactId) {
		final StringBuilder name = new StringBuilder();// artifact.getGroupId()).append(".");
		for (final String part : artifactId.split("-")) {
			name.append(StringUtils.capitalise(part));
		}
		return name.toString();
	}

	@Override
	protected File launchDirectory() {
		return this.launchDir.getAbsoluteFile();
	}

}
