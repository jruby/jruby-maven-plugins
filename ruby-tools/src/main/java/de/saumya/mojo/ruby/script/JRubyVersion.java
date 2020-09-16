package de.saumya.mojo.ruby.script;

import java.util.ArrayList;
import java.util.List;

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

    public boolean isVersionLowerThan(Integer... version) {
        final List<Integer> parts = extractVersionComponents(this.version);

        for (int i = 0; i < version.length && i < parts.size(); i++) {
            if (parts.get(i) == version[i])
                continue;
            return parts.get(i) < version[i];
        }
        return false;
    }

    private List<Integer> extractVersionComponents(String versionString) {
        if (versionString == null)
            throw new NumberFormatException();

        final String[] parts = versionString.split("\\.");

        final List<Integer> versionComponents = new ArrayList<Integer>();
        for (String part : parts) {
            versionComponents.add(Integer.parseInt(part));
        }
        return versionComponents;
    }

    public boolean isLanguageLowerThan(int major, int minor) {
        final List<Integer> parts = extractVersionComponents(language);

        int majorLanguageVersion = parts.get(0);
        int minorLanguageVersion = parts.get(1);

        if (major == majorLanguageVersion) {
            return minorLanguageVersion < minor;
        } else {
            return majorLanguageVersion < major;
        }
    }

}
