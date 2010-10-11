/**
 * 
 */
package de.saumya.mojo.ruby.rails;

import java.io.File;

import de.saumya.mojo.ruby.gems.GemsConfig;

public class RailsState {
    private final GemsConfig gemsConfig;
    private File                 launchDirectory;
    private boolean              patched = false;
    private String               model;

    public RailsState(final GemsConfig gemsConfig) {
        this.gemsConfig = gemsConfig;
        this.gemsConfig.setEnvironment("development");
    }

    void setLaunchDirectory(final File launchDirectory) {
        this.launchDirectory = launchDirectory;
    }

    public File getLaunchDirectory() {
        if (this.launchDirectory == null) {
            return new File(System.getProperty("user.dir"));
        }
        else {
            return this.launchDirectory;
        }
    }

    public boolean isPatched() {
        return this.patched;
    }

    public void setPatched(final boolean patched) {
        this.patched = patched;
    }

    @Override
    public RailsState clone() {
        final RailsState clone = new RailsState(this.gemsConfig.clone());
        clone.setLaunchDirectory(this.launchDirectory);
        clone.setModel(this.getModel());
        return clone;
    }

    public void setModel(final String model) {
        this.model = model;
    }

    public String getModel() {
        return this.model;
    }

    public GemsConfig getRubygemsConfig() {
        return this.gemsConfig;
    }
}