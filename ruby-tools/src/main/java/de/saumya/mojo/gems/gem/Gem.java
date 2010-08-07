package de.saumya.mojo.gems.gem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.saumya.mojo.gems.spec.GemSpecification;

/**
 * A Gem with specification and list of files.
 * 
 * @author mkristian
 */
public class Gem {

    private final List<GemFileEntry> files = new ArrayList<GemFileEntry>();

    private final GemSpecification   spec;

    public Gem(final GemSpecification spec) {
        this.spec = spec;
    }

    public static String constructGemFileName(final String gemName,
            final String gemVersion, final String platform) {
        final StringBuilder sb = new StringBuilder();

        // gemspec.name - gemspec.version - gemspec.platform ".gem"
        sb.append(gemName).append("-").append(gemVersion);

        if (platform != null && !"ruby".equals(platform)) {
            // only non Ruby platform should be appended
            sb.append("-").append(platform);
        }

        // extension
        sb.append(".gem");

        return sb.toString();
    }

    private String add(final File source, final String path) {
        if (!source.isFile()) {
            throw new RuntimeException("only files are implemented: " + source);
        }
        this.files.add(new GemFileEntry(source, path));
        return path;
    }

    public List<GemFileEntry> getGemFiles() {
        return this.files;
    }

    public void addFile(final File source) {
        addFile(source, source.getPath());
    }

    public void addFile(final File source, final String path) {
        this.spec.addFile(add(source, path));
    }

    public void addTestFile(final File source) {
        addTestFile(source, source.getPath());
    }

    public void addTestFile(final File source, final String path) {
        this.spec.addTestFile(add(source, path));
    }

    public void addExtraRdocFile(final File source) {
        addExtraRdocFile(source, source.getPath());
    }

    public void addExtraRdocFile(final File source, final String path) {
        this.spec.addExtraRdocFile(add(source, path));
    }

    public GemSpecification getSpecification() {
        return this.spec;
    }

    public String getGemFilename() {
        return constructGemFileName(this.spec.getName(), this.spec.getVersion()
                .toString(), this.spec.getPlatform());
    }
}
