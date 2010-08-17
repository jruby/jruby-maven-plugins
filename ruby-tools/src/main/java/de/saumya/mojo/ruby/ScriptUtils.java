/**
 * 
 */
package de.saumya.mojo.ruby;

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
        final InputStream stream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(name);

        if (stream == null) {
            throw new FileNotFoundException("loading resource from classloader failed: "
                    + name);
        }
        return stream;
    }

    public static InputStream getScriptAsStream(final String name,
            final Class<?> clazz) throws IOException {
        final InputStream stream = clazz.getResourceAsStream(name);
        if (stream == null) {
            return getScriptAsStream(name);
        }
        else {
            return stream;
        }
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