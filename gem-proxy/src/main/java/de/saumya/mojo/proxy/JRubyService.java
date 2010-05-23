/**
 * 
 */
package de.saumya.mojo.proxy;

import static org.jruby.embed.LocalContextScope.SINGLETON;
import static org.jruby.embed.LocalVariableBehavior.PERSISTENT;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.jruby.embed.ScriptingContainer;

class JRubyService {
    private final ScriptingContainer scriptingContainer;

    JRubyService() {
        this.scriptingContainer = new ScriptingContainer(SINGLETON,
                PERSISTENT);

        // setting the JRUBY_HOME to the one from the jruby jar - ignoring
        // the environment setting !
        this.scriptingContainer.getProvider()
                .getRubyInstanceConfig()
                .setJRubyHome(Thread.currentThread()
                        .getContextClassLoader()
                        .getResource("META-INF/jruby.home")
                        .toString()
                        .replaceFirst("^jar:", ""));

    }

    InputStream getResourceAsStream(final String name) throws IOException {
        InputStream stream = getClass().getResourceAsStream(name);
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

    Object rubyObject(final String name) throws IOException {
        return this.scriptingContainer.runScriptlet(getResourceAsStream(name),
                                                    name);
    }

    ScriptingContainer scripting() {
        return this.scriptingContainer;
    }
}