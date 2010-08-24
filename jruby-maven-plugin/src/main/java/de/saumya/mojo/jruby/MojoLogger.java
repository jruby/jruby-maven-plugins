/**
 * 
 */
package de.saumya.mojo.jruby;

import org.apache.maven.plugin.logging.Log;

import de.saumya.mojo.ruby.Logger;

public class MojoLogger implements Logger {

    private final boolean verbose;

    private final Log log;

    public MojoLogger(final boolean verbose, final Log log) {
        this.verbose = verbose;
        this.log = log;
    }

    public void debug(final CharSequence content) {
        if (this.verbose) {
            this.log.info(content);
        } else {
            this.log.debug(content);
        }
    }

    public void info(final CharSequence content) {
        this.log.info(content);
    }

    public void warn(final CharSequence content) {
        this.log.warn(content);
    }

    public void error(final CharSequence content) {
        this.log.error(content);
    }

}