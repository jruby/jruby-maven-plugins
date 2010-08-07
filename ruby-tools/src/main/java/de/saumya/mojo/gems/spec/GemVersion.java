package de.saumya.mojo.gems.spec;

/**
 * Gem::Version
 * 
 * @author cstamas
 */
public class GemVersion {
    private String version;

    public GemVersion() {
    }

    public GemVersion(final String version) {
        this.version = version;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return this.version;
    }
}
