package de.saumya.mojo.gem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import de.saumya.mojo.ruby.RubyScriptException;

/**
 * goal to convert that artifact into a gem.
 * 
 * @goal package
 * @requiresDependencyResolution test
 */
public class PackageMojo extends AbstractGemMojo {

	/**
	 * @parameter expression="${project.build.directory}"
	 */
	File buildDirectory;

	/**
	 * @parameter default-value="${gemspec}"
	 */
	File gemSpec;

	/**
	 * @parameter default-value="${gemspec.overwrite}"
	 */
	boolean gemspecOverwrite = false;

	/** @parameter */
	private String date;

	/** @parameter */
	private String extraRdocFiles;

	/** @parameter */
	private String extraFiles;

	/** @parameter */
	private String rdocOptions;

	/** @parameter */
	private String requirePaths;

	/** @parameter */
	private String rubyforgeProject;

	/** @parameter */
	private String rubygemsVersion;

	/** @parameter */
	private String requiredRubygemsVersion;

	/** @parameter */
	private String bindir;

	/** @parameter */
	private String requiredRubyVersion;

	/** @parameter */
	private String postInstallMessage;

	/** @parameter */
	private String executables;

	/** @parameter */
	private String extensions;

	/** @parameter */
	private String platform;

	/** @parameter default-value="gem_hook.rb"*/
	private String gemHook;

	private final Map<String, String> relocationMap = new HashMap<String, String>();

	/**
	 * @parameter expression="false"
	 */
	boolean includeDependencies;

	public void executeJRuby() throws MojoExecutionException, MojoFailureException, RubyScriptException {
		final MavenProject project = this.project;
		final GemArtifact artifact = new GemArtifact(project);
		try {
			if (this.gemSpec == null && project.getBasedir() != null && project.getBasedir().exists()) {
				// TODO generate the gemspec in the prepare-package phase so we
				// can use it separately
				build(project, artifact);
			} else {
				if (this.project.getBasedir() == null) {
					this.gemHome = new File(this.gemHome.getAbsolutePath().replace("/${project.basedir}/", "/"));
					this.gemPath = new File(this.gemPath.getAbsolutePath().replace("/${project.basedir}/", "/"));
				}
				if (this.gemSpec == null) {
					for (final File f : this.launchDirectory().listFiles()) {
						if (f.getName().endsWith(".gemspec")) {
							if (this.gemSpec == null) {
								this.gemSpec = f;
							} else {
								throw new MojoFailureException("more than one gemspec file found, use -Dgemspec=... to specifiy one");
							}
						}
					}
					if (this.gemSpec == null) {
						throw new MojoFailureException("no gemspec file or pom found, use -Dgemspec=... to specifiy a gemspec file or '-f ...' to use a pom file");
					} else {
						getLog().info("use gemspec: " + this.gemSpec);
					}
				}

				this.factory.newScriptFromResource(GEM_RUBY_COMMAND)
				                         .addArg("build", this.gemSpec)
				                         .executeIn(launchDirectory());
                //execute("-S gem build " + this.gemSpec.getAbsolutePath(), false);

				File gem = null;
				for (final File f : launchDirectory().listFiles()) {
					if (f.getName().endsWith(".gem")) {
						gem = f;
						break;
					}
				}
				if (project.getFile() != null && artifact.isGem()) {
					// only when the pom exist there will be an artifact
					FileUtils.copyFileIfModified(gem, artifact.getFile());
					gem.deleteOnExit();
				} else {
					// keep the gem where it when there is no buildDirectory
					if (this.buildDirectory.exists()) {
						FileUtils.copyFileIfModified(gem, new File(this.buildDirectory, gem.getName()));
						gem.deleteOnExit();
					}
				}
			}
		} catch (final IOException e) {
			throw new MojoExecutionException("error gemifing pom", e);
		}
	}

	private MavenProject projectFromArtifact(final Artifact artifact) throws ProjectBuildingException {
		final MavenProject project = this.builder.buildFromRepository(artifact, this.project.getRemoteArtifactRepositories(), this.localRepository);
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
	private void build(final MavenProject project, final GemArtifact artifact) throws MojoExecutionException, IOException, RubyScriptException {

		getLog().info("building gem for " + artifact + " . . .");
		getLog().info("include dependencies? " + this.includeDependencies);
		final File gemDir = new File(this.buildDirectory, artifact.getGemName());
		final File gemSpec = new File(gemDir, artifact.getGemName() + ".gemspec");
		final GemspecWriter gemSpecWriter = new GemspecWriter(gemSpec, project, artifact);

		if (this.date != null) {
			gemSpecWriter.append("date", Date.valueOf(this.date).toString());
		}
		gemSpecWriter.append("rubygems_version", this.rubygemsVersion);
		gemSpecWriter.append("required_rubygems_version", this.requiredRubygemsVersion);
		gemSpecWriter.append("required_ruby_version", this.requiredRubyVersion);
		gemSpecWriter.append("bindir", this.bindir);
		gemSpecWriter.append("post_install_message", this.postInstallMessage);

		gemSpecWriter.append("rubyforge_project", this.rubyforgeProject);
		gemSpecWriter.appendRdocFiles(this.extraRdocFiles);
		gemSpecWriter.appendFiles(this.extraFiles);
		gemSpecWriter.appendList("executables", this.executables);
		gemSpecWriter.appendList("extensions", this.extensions);
		gemSpecWriter.appendList("rdoc_options", this.rdocOptions);
		gemSpecWriter.appendList("require_paths", this.requirePaths);
		final File rubyFile;
		if (artifact.hasJarFile()) {
			gemSpecWriter.appendPlatform(this.platform == null ? "java" : this.platform);
			gemSpecWriter.appendJarfile(artifact.getJarFile(), artifact.getJarFile().getName());
			final File lib = new File(gemDir, "lib");
			lib.mkdirs();
			// need relative filename here
			rubyFile = new File(lib.getName(), artifact.getGemName() + ".rb");
			gemSpecWriter.appendFile(rubyFile);
		} else {
			rubyFile = null;
			gemSpecWriter.appendPlatform(this.platform);
		}

		ArtifactResolutionResult jarDependencyArtifacts = null;
		if (this.includeDependencies) {
			try {
				jarDependencyArtifacts = this.resolver.resolveTransitively(project.getArtifacts(), project.getArtifact(), this.project.getManagedVersionMap(),
						this.localRepository, this.project.getRemoteArtifactRepositories(), this.metadata, new ArtifactFilter() {
							public boolean include(final Artifact candidate) {
								if (candidate == artifact) {
									return true;
								}
								final boolean result = (candidate.getType().equals("jar") && ("compile".equals(candidate.getScope()) || "runtime".equals(candidate
										.getScope())));
								return result;
							}

						});
				for (final Iterator each = jarDependencyArtifacts.getArtifacts().iterator(); each.hasNext();) {
					final Artifact dependency = (Artifact) each.next();
					getLog().info(" -- include -- " + dependency);
					gemSpecWriter.appendJarfile(dependency.getFile(), dependency.getFile().getName());
				}
			} catch (final ArtifactResolutionException e) {
				e.printStackTrace();
			} catch (final ArtifactNotFoundException e) {
				e.printStackTrace();
			}
		}

		// TODO make it the maven way (src/main/ruby + src/test/ruby) or the
		// ruby way (lib + spec + test)
		// TODO make a loop or so ;-)
		final File binDir = new File(project.getBasedir(), "bin");
		final File libDir = new File(project.getBasedir(), "lib");
		final File generatorsDir = new File(project.getBasedir(), "generators");
		final File specDir = new File(project.getBasedir(), "spec");
		final File testDir = new File(project.getBasedir(), "test");

		if ( binDir.exists()) {
			gemSpecWriter.appendPath( "bin" );
			for ( File file : binDir.listFiles() ) {
				//if ( file.canExecute() ) { // java1.6 feature which will fail on jre1.5 runtimes
					gemSpecWriter.appendExecutable( file.getName() );
				//}
			}
		}
		if (libDir.exists()) {
			gemSpecWriter.appendPath("lib");
		}
		if (generatorsDir.exists()) {
			gemSpecWriter.appendPath("generators");
		}
		if (specDir.exists()) {
			gemSpecWriter.appendPath("spec");
			gemSpecWriter.appendTestPath("spec");
		}
		if (testDir.exists()) {
			gemSpecWriter.appendPath("test");
			gemSpecWriter.appendTestPath("test");
		}

		for (final Dependency dependency : project.getDependencies()) {
			if (!dependency.isOptional() && dependency.getType().contains("gem")) {
				if (!dependency.getVersion().matches(".*[\\)\\]]$")) {
					// it will adjust the artifact as well (in case of
					// relocation)

					Artifact arti = null;
					try {
						arti = this.artifactFactory.createArtifactWithClassifier(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency
								.getScope(), dependency.getClassifier());
						projectFromArtifact(arti);
						dependency.setGroupId(arti.getGroupId());
						dependency.setArtifactId(arti.getArtifactId());
					} catch (final ProjectBuildingException e) {
						throw new MojoExecutionException("error building project for " + arti, e);
					}
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
					for (final Iterator each = jarDependencyArtifacts.getArtifacts().iterator(); each.hasNext();) {
						final Artifact dependency = (Artifact) each.next();
						writer.append("  require File.dirname(__FILE__) + '/").append(dependency.getFile().getName()).append("'\n");
					}

				}
				writer.append("rescue LoadError\n");
				writer.append("  puts 'JAR-based gems require JRuby to load. Please visit www.jruby.org.'\n");
				writer.append("  raise\n");
				writer.append("end\n");
				writer.append("\n");
				writer.append("load File.dirname(__FILE__) + '/" + this.gemHook + "' if File.exists?( File.dirname(__FILE__) + '/" + this.gemHook +"')\n");
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

		final File localGemspec = new File(launchDirectory(), gemSpec.getName());

//		this.launchDirectory = gemDir;
//		execute("-S gem build " + gemSpec.getAbsolutePath());
		this.factory.newScriptFromResource(GEM_RUBY_COMMAND)
		                 .addArg("build", gemSpec)
		                 .executeIn(gemDir);

		if ((!localGemspec.exists() || !FileUtils.contentEquals(localGemspec, gemSpec)) && this.gemspecOverwrite) {
			getLog().info("overwrite gemspec '" + localGemspec.getName() + "'");
			FileUtils.copyFile(gemSpec, localGemspec);
		}

		final StringBuilder gemFilename = new StringBuilder("rubygems".equals(artifact.getGroupId()) ? "" : artifact.getGroupId() + ".").append(artifact.getArtifactId())
				.append("-").append(artifact.getGemVersion()).append("java-gem".equals(artifact.getType()) ? "-java" : "").append(".gem");

		FileUtils.copyFile(new File(gemDir, gemFilename.toString()), artifact.getFile());
	}

	private String titleizedClassname(final String artifactId) {
		final StringBuilder name = new StringBuilder();
		for (final String part : artifactId.split("-")) {
			name.append(StringUtils.capitalise(part));
		}
		return name.toString();
	}

    @Override
    protected void executeWithGems() throws MojoExecutionException,
            RubyScriptException, IOException {
        // TODO Auto-generated method stub
        
    }
}
