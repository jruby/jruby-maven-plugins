package de.saumya.mojo.gems.gem;

import java.io.File;

/**
 * A Gem file entry. It is sourced from a plain File and tells about where it
 * wants to be in Gem.
 * 
 * @author cstamas
 */
public class GemFileEntry {
    /**
     * The path where the file should be within Gem. Usually it is
     * "lib/theFileName.ext", but it may be overridden.
     */
    private String pathInGem;

    /**
     * The actual source of the file.
     */
    private File   source;

    public GemFileEntry(final File source, final String pathInGem) {
        this.source = source;

        this.pathInGem = pathInGem;
    }

    public String getPathInGem() {
        return this.pathInGem;
    }

    public void setPathInGem(final String pathInGem) {
        this.pathInGem = pathInGem;
    }

    public File getSource() {
        return this.source;
    }

    public void setSource(final File source) {
        this.source = source;
    }

}
