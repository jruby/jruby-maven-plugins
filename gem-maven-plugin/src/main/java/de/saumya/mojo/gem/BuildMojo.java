package de.saumya.mojo.gem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.codehaus.plexus.util.FileUtils;

import de.saumya.mojo.jruby.AbstractJRubyMojo;

/**
 * goal to run gem build
 * 
 * @goal build
 * @requiresProject true
 */
public class BuildMojo extends AbstractJRubyMojo {
    /**
     * 
     * @parameter
     */
    protected final List<Fileset> gemfilesets           = null;

    /**
     * map of dependency where the key is the gemname and value is the version
     * pattern
     * 
     * @parameter
     */
    protected Map<String, String> dependencies          = null;

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
     * @parameter default-value="${project.build.directory}/gemspec"
     */
    private final File            gemSpec               = null;

    /**
     * temporary directory to collect all the files for the gem
     * 
     * @parameter 
     *            default-value="${project.build.directory}/${project.artifactId}"
     */
    private final File            gemDirectory          = null;

    /**
     * file of the generated jar file
     * 
     * @parameter default-value=
     *            "${project.build.directory}/${project.build.finalName}.jar"
     */
    private final File            jarfile               = null;

    /**
     * file of the final gem.
     * 
     * @parameter default-value=
     *            "${project.build.directory}/${project.build.finalName}-java.gem"
     */
    private final File            gemFile               = null;

    static class GemspecWriter {

        final Writer     writer;
        final String     excludes       = ".*~$|^[.][a-z].*";
        final List<File> dirs           = new ArrayList<File>();
        final List<File> files          = new ArrayList<File>();

        long             latestModified = 0;

        private boolean  firstAuthor    = true;

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
            this.writer.append("\n");
        }

        void append(final String line) throws IOException {
            this.writer.append(line).append("\n");
        }

        void appendAuthor(final String name, final String email)
                throws IOException {
            if (this.firstAuthor) {
                this.writer.append("  s.authors = ['")
                        .append(name)
                        .append("']\n");
                this.writer.append("  s.email = ['")
                        .append(email)
                        .append("']\n");
                this.firstAuthor = false;
            }
            else {
                this.writer.append("  s.authors << '")
                        .append(name)
                        .append("'\n");
                this.writer.append("  s.email << '")
                        .append(email)
                        .append("'\n");
            }
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

        void appendDevelopmentDep(final String name, final String version)
                throws IOException {
            this.writer.append("  s.add_development_dependency '")
                    .append(name)
                    .append("', '")
                    .append(version)
                    .append("'\n");
        }

        void copyUrl(final File target, final String name, final URL url)
                throws IOException {
            OutputStream writer = null;
            InputStream reader = null;
            try {
                reader = new BufferedInputStream(url.openStream());
                writer = new BufferedOutputStream(new FileOutputStream(new File(target,
                        name)));
                int b = reader.read();
                while (b != -1) {
                    writer.write(b);
                    b = reader.read();
                }
            }
            finally {
                if (reader != null) {
                    try {
                        reader.close();
                    }
                    catch (final IOException ignore) {
                    }
                }
                if (writer != null) {
                    try {
                        writer.close();
                    }
                    catch (final IOException ignore) {
                    }
                }
            }
        }

        void appendPath(final String path) throws IOException {
            if (this.files.size() + this.dirs.size() == 0) {
                this.writer.append("  s.files = Dir['")
                        .append(path)
                        .append("/**/*']\n");
            }
            else {
                this.writer.append("  s.files += Dir['")
                        .append(path)
                        .append("/**/*']\n");
            }
            final File file = new File(path);
            if (file.lastModified() > this.latestModified) {
                this.latestModified = file.lastModified();
            }
            this.dirs.add(file);
        }

        void appendFile(final String file) throws IOException {
            if (this.files.size() + this.dirs.size() == 0) {
                this.writer.append("  s.files = Dir['")
                        .append(file)
                        .append("']\n");
            }
            else {
                this.writer.append("  s.files += Dir['")
                        .append(file)
                        .append("']\n");
            }
            final File f = new File(file);
            if (f.lastModified() > this.latestModified) {
                this.latestModified = f.lastModified();
            }
            this.files.add(f);
        }

        void copyFiles(final File target) throws IOException {
            for (final File file : this.files) {
                if (file.exists()) {
                    FileUtils.copyFile(file, new File(target, file.getPath()));
                }
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

    @SuppressWarnings("unchecked")
    public void execute() throws MojoExecutionException {
        this.gemSpec.getParentFile().mkdirs();
        // TODO use all license not only the first !!!
        final License license = (License) this.mavenProject.getLicenses()
                .get(0);
        final String licenseName;
        if (license != null) {
            licenseName = license.getUrl().replaceFirst(".*/", "");
        }
        else {
            licenseName = null;
        }
        final long lastGemTime = this.gemSpec.exists()
                ? this.gemSpec.lastModified()
                : 0;
        GemspecWriter writer = null;
        try {
            writer = new GemspecWriter(new FileWriter(this.gemSpec),
                    this.mavenProject);
            final FileSetManager fileSetManager = new FileSetManager();
            for (final Fileset fileset : this.gemfilesets) {
                getLog().info("copy from '"
                        + fileset.getDirectory()
                        + "': "
                        + Arrays.toString(fileSetManager.getIncludedFiles(fileset)));
                for (final String file : fileSetManager.getIncludedFiles(fileset)) {
                    writer.appendFile(fileset.getDirectory()
                            + File.separatorChar + file);
                }
            }
            if (licenseName != null) {
                writer.appendFile(licenseName);
            }
            if (this.dependencies != null) {
                for (final Map.Entry<String, String> entry : this.dependencies.entrySet()) {
                    writer.appendDep(entry.getKey(), entry.getValue());
                }
            }
            if (this.developmentDependencies != null) {
                for (final Map.Entry<String, String> entry : this.developmentDependencies.entrySet()) {
                    writer.appendDevelopmentDep(entry.getKey(),
                                                entry.getValue());
                }
            }
            for (final Developer developer : (List<Developer>) this.mavenProject.getDevelopers()) {
                writer.appendAuthor(developer.getName(), developer.getEmail());
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
        if (lastGemTime > writer.latestModified
                && lastGemTime > this.mavenProject.getFile().lastModified()) {
            getLog().info("gem up to date");
            return;
        }
        try {
            this.gemDirectory.mkdirs();
            writer.copyFiles(this.gemDirectory);
            writer.copyUrl(this.gemDirectory,
                           licenseName,
                           new URL(license.getUrl()));
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
