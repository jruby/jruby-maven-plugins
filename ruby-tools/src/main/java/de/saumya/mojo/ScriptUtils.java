/**
 * 
 */
package de.saumya.mojo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ScriptUtils {

    // do no initialize this
    private ScriptUtils() {
    }

    public static InputStream getScriptAsStream(final String name)
            throws IOException {
        InputStream stream = ScriptUtils.class.getResourceAsStream(name);
        if (stream == null) {
            stream = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(name);
        }
        if (stream == null) {
            throw new FileNotFoundException("loading resource from classloader failed: "
                    + name);
        }
        return stream;
    }

    public static URL getScript(final String name) throws IOException {
        URL url = ScriptUtils.class.getResource(name);
        if (url == null) {
            url = Thread.currentThread()
                    .getContextClassLoader()
                    .getResource(name);
        }
        if (url == null) {
            throw new FileNotFoundException("loading resource from classloader failed: "
                    + name);
        }
        return url;
    }
}