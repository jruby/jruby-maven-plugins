package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.codehaus.plexus.util.FileUtils;

import de.saumya.mojo.jruby.AbstractJRubyMojo;

/**
 * goal to build a gem from the given project. must be of type "gem". it takes
 * the metainfo from the pom alongs with extra configuration of the plugin to
 * build the gem.
 *
 * @goal build
 * @requiresProject true
 * @deprecated use package goal
 */
@Deprecated
public class BuildMojo extends AbstractJRubyMojo {
    /**
     *
     * @parameter
     */
    protected final List<Fileset> gemfilesets             = null;

    /**
     * map of dependency where the key is the gemname and value is the version
     * pattern
     *
     * @parameter
     */
    protected Map<String, String> dependencies            = null;

    /**
     * map of developer dependency where the key is the gemname and value is the
     * version pattern
     *
     * @parameter
     */
    protected Map<String, String> developmentDependencies = null;

    /**
     * the file location for the generated gemspec
     *
     * @parameter
     *            default-value="${project.build.directory}/${project.artifactId}.gemspec"
     */
    private final File            gemSpec                 = null;

    /**
     * temporary directory to collect all the files for the gem
     *
     * @parameter
     *            default-value="${project.build.directory}/${project.artifactId}"
     */
    private final File            gemDirectory            = null;

    /**
     * file of the generated jar file
     *
     * @parameter default-value=
     *            "${project.build.directory}/${project.build.finalName}.jar"
     */
    private final File            jarfile                 = null;

    /**
     * file of the final gem.
     *
     * @parameter default-value=
     *            "${project.build.directory}/${project.build.finalName}-java.gem"
     */
    private final File            gemFile                 = null;

    public void execute() throws MojoExecutionException {
        if (this.project.getArtifact().getType().equals("gem")) {
            build(this.dependencies,
                  this.developmentDependencies,
                  this.gemfilesets,
                  null,
                  this.project.getArtifactId() + "_ext.jar");
        }
        else {
            getLog().warn("building gem is configured but it is not a gem artifact. skip gem building");
        }
    }

    protected void build(final Map<String, String> dependencies,
            final Map<String, String> developmentDependencies,
            final List<Fileset> filesets, final String path,
            final String jarName) throws MojoExecutionException {
        GemspecWriter writer = null;
        try {
            writer = new GemspecWriter(this.gemSpec,
                    this.project,
                    new GemArtifact(this.project));
            if (path != null) {
                writer.appendPath(path);
            }
            if (filesets != null) {
                final FileSetManager fileSetManager = new FileSetManager(getLog());
                for (final Fileset fileset : filesets) {
                    getLog().info("copy from '"
                            + fileset.getDirectory()
                            + "': "
                            + Arrays.toString(fileSetManager.getIncludedFiles(fileset)));
                    for (final String file : fileSetManager.getIncludedFiles(fileset)) {
                        writer.appendFile(fileset.getDirectory()
                                + File.separatorChar + file);
                    }
                }
            }
            if (dependencies != null) {
                for (final Map.Entry<String, String> entry : dependencies.entrySet()) {
                    writer.appendDependency(entry.getKey(), entry.getValue());
                }
            }
            if (developmentDependencies != null) {
                for (final Map.Entry<String, String> entry : developmentDependencies.entrySet()) {
                    writer.appendDevelopmentDependency(entry.getKey(),
                                                       entry.getValue());
                }
            }
            if (this.jarfile.exists()) {
                writer.appendJarfile(this.jarfile, jarName);
            }
        }
        catch (final IOException e) {
            throw new MojoExecutionException("error writing out the gemspec file",
                    e);
        }
        finally {
            if (writer != null) {
                try {
                    writer.close();
                }
                catch (final IOException e) {
                    // ignore
                }
            }
        }
        if (writer.isUptodate() && this.gemFile.exists()) {
            getLog().info("gem up to date");
            return;
        }
        try {
            // this.gemDirectory.mkdirs();
            writer.copy(this.gemDirectory);
            // FileUtils.copyFile(this.jarfile,
            // new File(new File(this.gemDirectory, "lib"),
            // jarName));
        }
        catch (final IOException e) {
            throw new MojoExecutionException("error copying files", e);
        }
        execute("-S gem build " + this.gemSpec.getAbsolutePath());
        try {
            FileUtils.copyFile(new File(this.gemDirectory,
                                       this.gemFile.getName()
                                               .replace("-SNAPSHOT", "")),
                               this.gemFile);
        }
        catch (final IOException e) {
            throw new MojoExecutionException("error copying files", e);
        }
    }

    @Override
    protected File launchDirectory() {
        return this.gemDirectory.getAbsoluteFile();
    }

}
