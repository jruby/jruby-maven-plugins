package de.saumya.mojo.proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;

public abstract class RubygemsHtmlVisitor {
    
    private StringBuilder currentListElement = null;

    private final Set<String> versions = new TreeSet<String>();

    private final boolean prereleases;

    private Set<String> brokenVersions;

    protected String gemname;

    public RubygemsHtmlVisitor(String gemname, boolean prereleases, Set<String> brokenVersions) {
        this.gemname = gemname;
        this.prereleases = prereleases;
        this.brokenVersions = brokenVersions;
    }

    public void accept(URL url) throws IOException{
        accept(new BufferedReader(new InputStreamReader(url.openStream())));
    }

    private void accept(BufferedReader reader) throws IOException {
        while(true){
            String line = reader.readLine();
            if(line == null){
                reader.close();
                return;
            }
            else {
                visit(line);
            }
        }
    }

    private void visit(String line) {
        if(currentListElement == null) {
            if(line.contains("<li")){
                line = line.replaceFirst("^.*<li", "<li");
                if(line.contains("</li>")){
                    checkLine(line.replace("</li>.*$", ""));
                }
                else{
                    currentListElement = new StringBuilder(line);
                }
            }
        }
        else {
            if(line.contains("</li>")){
                line = line.replace("</li>.*$", "");
                currentListElement.append(line);
                checkLine(currentListElement.toString());
                currentListElement = null;
            }
            else {
                currentListElement.append(line);
            }
        }
    }

    private void checkLine(String versionLine) {
        if(!versionLine.contains("yanked") && !versionLine.contains("x86-m")){
            String version = versionLine.replaceFirst("</a>.*$", "")
                    .replaceFirst("^.*>", "").trim();
            if (((!prereleases && version.matches("^[0-9]+(\\.[0-9]+)*$"))
                    || (prereleases
                            && version.matches("^[0-9]+(\\.[0-9a-zA-Z]+)*$") && version.matches(".*[a-zA-Z].*")))
                    && !versions.contains(version) && (brokenVersions == null || !brokenVersions.contains(version))) {
                addVersion(version);
                versions.add(version);
            }
        }
    }

    abstract protected void addVersion(String version);
}
