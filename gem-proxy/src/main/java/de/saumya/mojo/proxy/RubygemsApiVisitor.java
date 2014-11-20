package de.saumya.mojo.proxy;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public abstract class RubygemsApiVisitor {

    private final Set<String> versions = new TreeSet<String>();

    private final boolean prereleases;

    private Set<String> brokenVersions;

    protected String gemname;

    public RubygemsApiVisitor(String gemname, boolean prereleases, Set<String> brokenVersions) {
        this.gemname = gemname;
        this.prereleases = prereleases;
        this.brokenVersions = brokenVersions;
    }

    public void accept(URL url) throws IOException{
        accept(new BufferedReader(new InputStreamReader(url.openStream())));
    }

    @SuppressWarnings("unchecked")
    private void accept(BufferedReader reader) throws IOException {
        Yaml yaml = new Yaml(new SafeConstructor());
        try {
            List<Map<String, Object>> versionsYaml = (List<Map<String, Object>>) yaml.load(reader);
            for (Map<String, Object> versionYaml : versionsYaml) {
                String number = versionYaml.get("number").toString();
                String platform = versionYaml.get("platform").toString();
                boolean prerelease = (Boolean) versionYaml.get("prerelease");
                if ((!prereleases && !prerelease) || (prereleases && prerelease)) {
                    if (!versions.contains(number) && (brokenVersions == null || !brokenVersions.contains(number))
                            && !platform.contains("x86-m")) {
                        if (prereleases) {
                            number += "-SNAPSHOT";
                        }
                        addVersion(number);
                        versions.add(number);
                    }
                }
            }
        } finally {
            reader.close();
        }
    }

    abstract protected void addVersion(String version);
}
