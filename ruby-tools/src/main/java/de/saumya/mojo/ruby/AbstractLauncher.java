package de.saumya.mojo.ruby;


public abstract class AbstractLauncher implements Launcher {

    private final Log log;

    AbstractLauncher(final Log log) {
        this.log = log;
    }

    protected Log getLog() {
        return this.log;
    }

}
