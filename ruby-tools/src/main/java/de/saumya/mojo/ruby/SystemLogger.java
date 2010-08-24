/**
 * 
 */
package de.saumya.mojo.ruby;

public class SystemLogger implements Logger {

    private final boolean verbose;

    public SystemLogger() {
        this(true);
    }

    public SystemLogger(final boolean verbose) {
        this.verbose = verbose;
    }

    public void debug(final CharSequence content) {
        if (this.verbose) {
            System.out.append(content);
        }
    }

    public void info(final CharSequence content) {
        System.out.append(content);
    }

    public void warn(final CharSequence content) {
        System.err.append(content);
    }

    public void error(final CharSequence content) {
        System.err.append(content);
    }
}