/**
 * 
 */
package de.saumya.mojo.ruby.gems;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GemsConfig {

    private static final String GEM_PATH         = "GEM_PATH";
    private static final String GEM_HOME         = "GEM_HOME";

    private String              env;

    private File                gemBase;

    private File                gemHome;

    private List<File>          gemPaths         = new ArrayList<File>();

    private File[]              gemsDirectory;

    private File                binDirectory;

    private boolean             addRI            = false;

    private boolean             addRdoc          = false;

    private boolean             verbose          = false;

    private boolean             userInstall      = false;

    private boolean             systemInstall    = false;

    private boolean             skipJRubyOpenSSL = false;

    public void setSkipJRubyOpenSSL(final boolean skip) {
        this.skipJRubyOpenSSL = skip;
    }

    public boolean skipJRubyOpenSSL() {
        return this.skipJRubyOpenSSL;
    }

    public void setAddRI(final boolean addRI) {
        this.addRI = addRI;
    }

    public boolean isAddRI() {
        return this.addRI;
    }

    public void setAddRdoc(final boolean addRdoc) {
        this.addRdoc = addRdoc;
    }

    public boolean isAddRdoc() {
        return this.addRdoc;
    }

    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public void setUserInstall(final boolean userInstall) {
        this.userInstall = userInstall;
    }
    
    public void setSystemInstall(final boolean systemInstall) {
        this.systemInstall = systemInstall;
    }

    public boolean isUserInstall() {
        return this.userInstall;
    }

    public boolean isSystemInstall() {
        return this.systemInstall;
    }

    public File[] getGemsDirectory() {
        if (this.gemsDirectory == null) {
            File[] paths = getGemPath();
            this.gemsDirectory = new File[paths.length];
            int index = 0;
            for(File path: paths){
                this.gemsDirectory[index++] = new File(path, "gems"); 
            }
        }
        return this.gemsDirectory;
    }

    
    public void setBinDirectory(File binDirectory) {
        this.binDirectory = binDirectory;
    }
    
    public File getBinDirectory() {
        if (this.binDirectory == null) {
            if(getGemHome() != null){
                return new File(getGemHome(), "bin");
            }
            else {
                return null;
            }
        }
        return this.binDirectory;
    }

    public File binScriptFile(final String scriptName) {
        if (getBinDirectory() == null){
            // TODO something better
            return new File(scriptName);          
        }
        else {
            return new File(getBinDirectory(), scriptName);
        }
    }

    public String getEnvironment() {
        return this.env;
    }

    public void setEnvironment(final String env) {
        this.env = env;
        setGemBase(this.gemBase);
    }

    public void setGemBase(final File base) {
        this.gemBase = base;
        if (this.gemBase != null) {
            final String postfix = this.env == null ? "" : "-" + this.env;
            this.gemHome = new File(this.gemBase.getPath() + postfix);
            this.gemPaths.set(0, new File(this.gemBase.getPath() + postfix));
        }
    }

    public boolean hasGemBase() {
        return this.gemBase != null;
    }

    public void setGemHome(final File home) {
        this.gemHome = home;
        this.gemBase = null;
    }

    public void addGemPath(final File path) {
        if( path != null ){
            this.gemPaths.add(path);
            this.gemBase = null;
            this.gemsDirectory = null;
        }
    }

    public File getGemHome() {
        if (this.gemHome == null || systemInstall) {
            if (System.getenv(GEM_HOME) == null) {
                return null;
            }
            else {
                return new File(System.getenv(GEM_HOME));
            }
        }
        else {
            return this.gemHome;
        }
    }

    public File[] getGemPath() {
        if (this.gemPaths.size() == 0 || systemInstall) {
            if (System.getenv(GEM_PATH) == null) {
                return null;
            }
            else {
                return new File[] {new File(System.getenv(GEM_PATH))};
            }
        }
        else {
            return this.gemPaths.toArray(new File[this.gemPaths.size()]);
        }
    }

    @Override
    public GemsConfig clone() {
        final GemsConfig clone = new GemsConfig();
        clone.setEnvironment(this.env);
        if (this.gemBase != null) {
            clone.setGemBase(this.gemBase);
        }
        else {
            clone.setGemHome(this.gemHome);
            for(File path: this.gemPaths){
                clone.addGemPath(path);
            }
        }
        clone.addRdoc = this.addRdoc;
        clone.addRI = this.addRI;
        clone.userInstall = this.userInstall;
        clone.systemInstall = this.systemInstall;
        clone.verbose = this.verbose;
        clone.skipJRubyOpenSSL = this.skipJRubyOpenSSL;
        clone.binDirectory = this.binDirectory;

        return clone;
    }

}