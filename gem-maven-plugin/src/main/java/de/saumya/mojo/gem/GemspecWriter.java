/**
 * 
 */
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Contributor;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;


class GemspecWriter {

    final Writer            writer;
    final String            excludes       = ".*~$|^[.][a-zA-Z].*";
    final List<File>        dirs           = new ArrayList<File>();
    final List<File>        files          = new ArrayList<File>();
    final List<URL>         licenses       = new ArrayList<URL>();
    final Map<String, File> jarFiles       = new HashMap<String, File>();
    long                    latestModified = 0;
    final File              gemspec;
    private boolean         firstAuthor    = true;
    private boolean         firstFile      = true;

    @SuppressWarnings("unchecked")
    GemspecWriter(final File gemspec, final MavenProject project,
            final GemArtifact artifact) throws IOException {
        this.latestModified = project.getFile() == null ? 0 : project.getFile()
                .lastModified();
        this.gemspec = gemspec;
        this.gemspec.getParentFile().mkdirs();
        this.writer = new FileWriter(gemspec);

        append("Gem::Specification.new do |s|");
        append("name", artifact.getGemName());
        append("version", gemVersion(project.getVersion()));
        append();
        append("summary", project.getName());
        append("description", project.getDescription());
        append("homepage", project.getUrl());
        append();

        for (final Developer developer : project.getDevelopers()) {
            appendAuthor(developer.getName(), developer.getEmail());
        }
        for (final Contributor contributor : project.getContributors()) {
            appendAuthor(contributor.getName(), contributor.getEmail());
        }
        append();

        for (final License license : project.getLicenses()) {
            appendLicense(license.getUrl(), license.getName());
        }
    }

    boolean isUptodate() {
        return this.gemspec.lastModified() > this.latestModified;
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

    private void append() throws IOException {
        this.writer.append("\n");
    }

    private void append(final String line) throws IOException {
        this.writer.append(line).append("\n");
    }

    private void appendAuthor(final String name, final String email)
            throws IOException {
        if (name != null && email != null) {
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
    }

    private void append(final String key, final String value)
            throws IOException {
        if (value != null) {
            this.writer.append("  s.")
                    .append(key)
                    .append(" = '")
                    .append(value.replaceAll("'", "\""))
                    .append("'\n");
        }
    }

    void appendDependency(final String name, final String version)
            throws IOException {
        this.writer.append("  s.add_dependency '")
                .append(name)
                .append("', '")
                .append(gemVersion(version))
                .append("'\n");
    }

    void appendDevelopmentDependency(final String name, final String version)
            throws IOException {
        this.writer.append("  s.add_development_dependency '")
                .append(name)
                .append("', '")
                .append(gemVersion(version))
                .append("'\n");
    }

    void appendPath(final String path) throws IOException {
        if (this.firstFile) {
            this.writer.append("  s.files = Dir['")
                    .append(path)
                    .append("/**/*']\n");
            this.firstFile = false;
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

    void appendJarfile(final File jar, final String jarfileName)
            throws IOException {
        append("platform", "java");
        final File f = new File("lib", jarfileName);
        this.jarFiles.put(f.toString(), jar);
        appendFile(f);
    }

    void appendFile(final File file) throws IOException {
        if (this.firstFile) {
            this.writer.append("  s.files = Dir['")
                    .append(file.toString())
                    .append("']\n");
            this.firstFile = false;
        }
        else {
            this.writer.append("  s.files += Dir['")
                    .append(file.toString())
                    .append("']\n");
        }
        if (file.lastModified() > this.latestModified) {
            this.latestModified = file.lastModified();
        }
    }

    void appendFile(final String file) throws IOException {
        // if (this.files.size() + this.dirs.size() == 0) {
        // this.writer.append("  s.files = Dir['").append(file).append("']\n");
        // }
        // else {
        // this.writer.append("  s.files += Dir['")
        // .append(file)
        // .append("']\n");
        // }
        // if (f.lastModified() > this.latestModified) {
        // this.latestModified = f.lastModified();
        // }
        final File f = new File(file);
        appendFile(f);
        this.files.add(f);
    }

    private void appendLicense(final String url, final String name)
            throws IOException {
        URL u;
        try {
            u = new URL(url);
        }
        catch (final MalformedURLException e) {
            u = new URL("file:." + url);
        }
        this.licenses.add(u);
        final URLConnection con = u.openConnection();
        if (this.latestModified < con.getLastModified()) {
            this.latestModified = con.getLastModified();
        }
        appendFile(new File(u.getFile().substring(1))); // omit the first slash
        if (name != null) {
            append("  s.licenses << '" + name + "'");
        }
    }

    void copy(final File target) throws IOException {
        target.mkdirs();
        copyJarFiles(target);
        copyFiles(target);
        copyLicenses(target);
    }

    private void copyLicenses(final File target) throws IOException {
        OutputStream writer = null;
        InputStream reader = null;
        for (final URL url : this.licenses) {
            try {
                try {
                    reader = new BufferedInputStream(url.openStream());
                }
                catch (final IOException e) {
                    // TODO log it but otherwise ignore
                    break;
                }
                final File licenseFile = new File(target, url.getFile());
                licenseFile.getParentFile().mkdirs();
                writer = new BufferedOutputStream(new FileOutputStream(licenseFile));
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
    }

    private void copyJarFiles(final File target) throws IOException {
        new File(target, "lib").mkdirs();
        for (final Map.Entry<String, File> entry : this.jarFiles.entrySet()) {
            FileUtils.copyFile(entry.getValue(), new File(target,
                    entry.getKey()));
        }
    }

    private void copyFiles(final File target) throws IOException {
        for (final File file : this.files) {
            if (file.exists()) {
                FileUtils.copyFile(file, new File(target, file.getPath()));
            }
        }
        for (final File dir : this.dirs) {
            copyDir(target, dir);
        }
    }

    private void copyDir(final File target, final File dir) throws IOException {
        if (dir.isDirectory()) {
            for (final String file : dir.list()) {
                copyDir(target, new File(dir, file));
            }
        }
        else {
            if (dir.exists() && !dir.getName().matches(this.excludes)) {
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