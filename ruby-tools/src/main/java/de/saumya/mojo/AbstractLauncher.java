package de.saumya.mojo;

public abstract class AbstractLauncher implements Launcher {

    private final Log log;

    AbstractLauncher(final Log log) {
        this.log = log;
    }

    protected Log getLog() {
        return this.log;
    }

}
