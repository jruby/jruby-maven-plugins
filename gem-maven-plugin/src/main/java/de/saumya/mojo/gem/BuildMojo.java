package de.saumya.mojo.gem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Developer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import de.saumya.mojo.jruby.AbstractJRubyMojo;

/**
 * goal to run gem build
 * 
 * @goal build
 */
public class BuildMojo extends AbstractJRubyMojo {
    /**
     * list of comma separated gem names.
     * 
     * @parameter default-value="${project.build.directory}/gemspec"
     */
    private final File gemSpec      = null;

    /**
     * list of comma separated gem names.
     * 
     * @parameter 
     *            default-value="${project.build.directory}/${project.artifactId}"
     */
    private final File gemDirectory = null;

    /**
     * list of comma separated gem names.
     * 
     * @parameter default-value=
     *            "${project.build.directory}/${project.build.finalName}.jar"
     */
    private final File jarfile      = null;

    /**
     * @parameter default-value=
     *            "${project.build.directory}/${project.build.finalName}-java.gem"
     */
    private final File gemFile      = null;

    static class GemspecWriter {

        final Writer     writer;
        final String     excludes = ".*~$|^[.][a-z].*";
        final List<File> dirs     = new ArrayList<File>();
        final List<File> files    = new ArrayList<File>();

        GemspecWriter(final Writer writer, final MavenProject project)
                throws IOException {
            this.writer = writer;
            append("Gem::Specification.new do |s|");
            append("name", project.getArtifactId());
            append("version", project.getVersion().replace("-SNAPSHOT", ""));
            append();
            append("summary", project.getName());
            append("description", project.getDescription());
            append("platform", "java");
            append("homepage", project.getUrl());
            append();
            for (final Object object : project.getDevelopers()) {
                final Developer developer = (Developer) object;
                developer.getName();
                developer.getEmail();
            }
        }

        void append() throws IOException {
            append("");
        }

        void append(final String line) throws IOException {
            this.writer.append(line).append("\n");
        }

        void append(final String key, final String value) throws IOException {
            if (value != null) {
                this.writer.append("  s.")
                        .append(key)
                        .append(" = '")
                        .append(value)
                        .append("'\n");
            }
        }

        void appendDep(final String name, final String version)
                throws IOException {
            this.writer.append("  s.add_dependency '")
                    .append(name)
                    .append("', '")
                    .append(version)
                    .append("'\n");
        }

        void appendDeveloperDep(final String name, final String version)
                throws IOException {
            this.writer.append("  s.add_development_dependency '")
                    .append(name)
                    .append("', '")
                    .append(version)
                    .append("'\n");
        }

        void appendPath(final String path) throws IOException {
            if (this.dirs.size() == 0) {
                this.writer.append("  s.files = Dir['")
                        .append(path)
                        .append("/**/*']\n");
            }
            else {
                this.writer.append("  s.files += Dir['")
                        .append(path)
                        .append("/**/*']\n");
            }
            this.dirs.add(new File(path));
        }

        void appendFile(final String file) throws IOException {
            this.writer.append("  s.files += Dir['")
                    .append(file)
                    .append("']\n");
            this.files.add(new File(file));
        }

        void copyFiles(final File target) throws IOException {
            for (final File file : this.files) {
                FileUtils.copyFile(file, new File(target, file.getPath()));
            }
            for (final File dir : this.dirs) {
                copyDir(target, dir);
            }
        }

        void copyDir(final File target, final File dir) throws IOException {
            if (dir.isDirectory()) {
                for (final String file : dir.list()) {
                    copyDir(target, new File(dir, file));
                }
            }
            else {
                if (!dir.getName().matches(this.excludes)) {
                    FileUtils.copyFile(dir, new File(target, dir.getPath()));
                }
            }
        }

        void close() throws IOException {
            try {
                this.writer.append("end");
            }
            finally {
                this.writer.close();
            }
        }
    }

    public void execute() throws MojoExecutionException {
        this.gemSpec.getParentFile().mkdirs();
        GemspecWriter writer = null;
        try {
            writer = new GemspecWriter(new FileWriter(this.gemSpec),
                    this.mavenProject);
            writer.appendPath("lib");
            writer.appendPath("spec");
            writer.appendFile("MIT-LICENSE");
            writer.appendDep("dm-core", "0.10.1");
            writer.appendDeveloperDep("rspec", "~>1.2");
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
        try {
            this.gemDirectory.mkdirs();
            writer.copyFiles(this.gemDirectory);
            FileUtils.copyFile(this.jarfile,
                               new File(new File(this.gemDirectory, "lib"),
                                       this.mavenProject.getArtifactId()
                                               + "_ext.jar"));
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
