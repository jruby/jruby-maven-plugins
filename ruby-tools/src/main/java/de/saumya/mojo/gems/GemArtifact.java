package de.saumya.mojo.gems;

import java.io.File;

import de.saumya.mojo.gems.spec.GemSpecification;

/**
 * The response of the converter: gempsec file and the actual File where Gem is
 * saved.
 * 
 * @author cstamas
 */
public class GemArtifact {

    private final GemSpecification gemspec;

    private final File             gemFile;

    public GemArtifact(final GemSpecification gemspec, final File gemFile) {
        this.gemspec = gemspec;

        this.gemFile = gemFile;
    }

    public GemSpecification getGemspec() {
        return this.gemspec;
    }

    public File getGemFile() {
        return this.gemFile;
    }
}
