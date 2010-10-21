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

    final MavenProject      project;
    final Writer            writer;
    final String            excludes         = ".*~$|^[.][a-zA-Z].*";
    final List<File>        dirs             = new ArrayList<File>();
    final List<File>        files            = new ArrayList<File>();
    final List<URL>         licenses         = new ArrayList<URL>();
    final Map<String, File> jarFiles         = new HashMap<String, File>();
    long                    latestModified   = 0;
    final File              gemspec;
    private boolean         firstAuthor      = true;
    private boolean         firstFile        = true;
    private boolean         platformAppended = false;
    private boolean         firstTestFile;

    // private List<String> executables = new ArrayList<String>();

    GemspecWriter(final File gemspec, final MavenProject project,
            final GemArtifact artifact) throws IOException {
        this.latestModified = project.getFile() == null ? 0 : project.getFile()
                .lastModified();
        this.gemspec = gemspec;
        this.gemspec.getParentFile().mkdirs();
        this.writer = new FileWriter(gemspec);
        this.project = project;

        append("# create by maven - leave it as is");
        append("Gem::Specification.new do |s|");
        append("name", artifact.getGemName());
        appendRaw("version", "'"
                + GemArtifact.getGemVersion(project.getVersion()) + "'");
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

    private String gemVersion(String version) {
        version = version.replaceAll("-SNAPSHOT", "").replace("-", ".");
        if (version.matches("^[\\[\\(].*[\\]\\)]$")) {
            final int comma = version.indexOf(",");
            final String first = version.substring(1, comma);
            final String second = version.substring(comma + 1,
                                                    version.length() - 1);
            if (version.matches("\\[.*99999.99999\\)$")) {
                // out of '[1.2.0, 1.99999.99999]' make '1.2'
                final String prefix = second.replaceFirst("99999.99999$", "");
                return "'~> "
                        + prefix
                        + first.substring(prefix.length())
                                .replaceFirst("[.].*", "") + "'";
            }
            else if (version.matches("\\[.*,\\)$")) {
                final StringBuilder buf = new StringBuilder("'>");
                buf.append(version.charAt(0) == '[' ? "=" : "");
                buf.append(first).append("'");
                return buf.toString();
            }
            else {
                final StringBuilder buf = new StringBuilder("['>");
                buf.append(version.charAt(0) == '[' ? "=" : "");
                buf.append(first).append("','<");
                buf.append(version.charAt(version.length() - 1) == '['
                        ? "="
                        : "");
                buf.append(second);
                buf.append("']");
                return buf.toString();
            }
        }
        else {
            return "'" + version + "'";
        }
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

    void append(final String key, final String value) throws IOException {
        if (value != null) {
            this.writer.append("  s.")
                    .append(key)
                    .append(" = '")
                    .append(value.replaceAll("'", "\""))
                    .append("'\n");
        }
    }

    private void appendRaw(final String key, final String value)
            throws IOException {
        if (value != null) {
            this.writer.append("  s.")
                    .append(key)
                    .append(" = ")
                    .append(value)
                    .append("\n");
        }
    }

    void appendDependency(final String name, final String version)
            throws IOException {
        this.writer.append("  s.add_dependency '")
                .append(name)
                .append("', ")
                .append(gemVersion(version))
                .append("\n");
    }

    void appendDevelopmentDependency(final String name, final String version)
            throws IOException {
        this.writer.append("  s.add_development_dependency '")
                .append(name)
                .append("', ")
                .append(gemVersion(version))
                .append("\n");
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
        final File file = new File(this.project.getBasedir(), path);
        if (file.lastModified() > this.latestModified) {
            this.latestModified = file.lastModified();
        }
        this.dirs.add(new File(path));
    }

    void appendTestPath(final String path) throws IOException {
        if (this.firstTestFile) {
            this.writer.append("  s.test_files = Dir['")
                    .append(path)
                    .append("/**/*_" + path + ".rb']\n");
            this.firstTestFile = false;
        }
        else {
            this.writer.append("  s.test_files += Dir['")
                    .append(path)
                    .append("/**/*_" + path + ".rb']\n");
        }
        final File file = new File(this.project.getBasedir(), path);
        if (file.lastModified() > this.latestModified) {
            this.latestModified = file.lastModified();
        }
        this.dirs.add(new File(path));
    }

    void appendPlatform(final String platform) throws IOException {
        if (!this.platformAppended && platform != null) {
            append("platform", platform);
            this.platformAppended = true;
        }
    }

    void appendJarfile(final File jar, final String jarfileName)
            throws IOException {
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
        final File f = new File(file);
        appendFile(f);
        this.files.add(f);
    }

    void appendExecutable(final String executable) throws IOException {
        this.writer.append("  s.executables << '" + executable + "'\n");
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
        // omit the first slash
        final File license = new File(u.getFile()
                .substring(1)
                .replaceFirst("^./", ""));
        appendFile(license);
        if ("file".equals(u.getProtocol())) {
            // make a nice File without unix style prefix "./"
            this.files.add(new File(license.getPath().replaceFirst("^./", "")));
        }
        if (name != null) {
            append("  s.licenses << '" + name.replaceFirst("^./", "") + "'");
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
        final File realDir = new File(this.project.getBasedir(), dir.getPath());
        if (realDir.isDirectory()) {
            for (final String file : realDir.list()) {
                copyDir(target, new File(dir, file));
            }
        }
        else {
            if (realDir.exists() && !realDir.getName().matches(this.excludes)) {
                final File targetFile = new File(target, dir.getPath());
                FileUtils.copyFile(realDir, targetFile);
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

    void appendList(final String name, final String list) throws IOException {
        if (list != null) {
            final StringBuilder buf = new StringBuilder("[");
            boolean first = true;
            for (final String part : list.split(",")) {
                if (first) {
                    first = false;
                }
                else {
                    buf.append(",");
                }
                final char quoteChar = part.contains("'") ? '"' : '\'';
                buf.append(quoteChar).append(part.trim()).append(quoteChar);
            }
            buf.append("]");
            appendRaw(name, buf.toString());
        }
    }

    void appendRdocFiles(final String extraRdocFiles) throws IOException {
        if (extraRdocFiles != null) {
            for (final String f : extraRdocFiles.split(",")) {
                appendFile(f.trim());
            }
            appendList("extra_rdoc_files", extraRdocFiles);
        }
    }

    void appendFiles(final String files) throws IOException {
        if (files != null) {
            for (final String f : files.split(",")) {
                appendFile(f.trim());
            }
        }
    }
}
