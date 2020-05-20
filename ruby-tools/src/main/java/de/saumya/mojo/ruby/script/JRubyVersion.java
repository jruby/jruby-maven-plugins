package de.saumya.mojo.ruby.script;

public class JRubyVersion {

    private final String version;
    private final String language;

    public JRubyVersion(String version, String language) {
        this.version = version;
        this.language = language;
    }

    public String getVersion() {
        return version;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isLanguageLowerThan(int major, int minor) {
        final String[] parts = getLanguage().split("\\.");

        if (major < Integer.parseInt(parts[0]))
            return true;

        if (minor < Integer.parseInt(parts[1]))
            return true;

        return false;
    }

}
