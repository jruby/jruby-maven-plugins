/**
 * 
 */
package de.saumya.mojo.ruby.gems;

import java.io.File;

public class GemsConfig {

    private static final String GEM_PATH         = "GEM_PATH";
    private static final String GEM_HOME         = "GEM_HOME";

    private String              env;

    private File                gemBase;

    private File                gemHome;

    private File                gemPath;

    private File                gemsDirectory;

    private File                binDirectory;

    private boolean             addRI            = false;

    private boolean             addRdoc          = false;

    private boolean             verbose          = false;

    private boolean             userInstall      = false;

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

    public boolean isUserInstall() {
        return this.userInstall;
    }

    public File getGemsDirectory() {
        if (this.gemsDirectory == null) {
            this.gemsDirectory = new File(getGemPath(), "gems");
        }
        return this.gemsDirectory;
    }

    public File getBinDirectory() {
        if (this.binDirectory == null) {
            this.binDirectory = new File(getGemHome(), "bin");
        }
        return this.binDirectory;
    }

    public File binScriptFile(final String scriptName) {
        return new File(getBinDirectory(), scriptName);
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
            this.gemPath = new File(this.gemBase.getPath() + postfix);
        }
    }

    public boolean hasGemBase() {
        return this.gemBase != null;
    }

    public void setGemHome(final File home) {
        this.gemHome = home;
        this.gemBase = null;
    }

    public void setGemPath(final File base) {
        this.gemPath = base;
        this.gemBase = null;
    }

    public File getGemHome() {
        if (this.gemHome == null) {
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

    public File getGemPath() {
        if (this.gemPath == null) {
            if (System.getenv(GEM_PATH) == null) {
                return null;
            }
            else {
                return new File(System.getenv(GEM_PATH));
            }
        }
        else {
            return this.gemPath;
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
            clone.setGemPath(this.gemPath);
        }
        clone.addRdoc = this.addRdoc;
        clone.addRI = this.addRI;
        clone.userInstall = this.userInstall;
        clone.verbose = this.verbose;
        clone.skipJRubyOpenSSL = this.skipJRubyOpenSSL;

        return clone;
    }
}