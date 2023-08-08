/**
 * 
 */
package de.saumya.mojo.ruby;

public interface Logger {

    /**
     * Send a message to the user in the <b>debug</b> level.
     * 
     * @param content
     */
    void debug(CharSequence content);

    /**
     * Send a message to the user in the <b>info</b> level.
     * 
     * @param content
     */
    void info(CharSequence content);

    /**
     * Send a message to the user in the <b>warn</b> level.
     * 
     * @param string
     */
    void warn(CharSequence string);

    /**
     * Send a message to the user in the <b>error</b> level.
     * 
     * @param string
     */
    void error(CharSequence string);
}