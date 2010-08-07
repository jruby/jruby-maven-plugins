/**
 * 
 */
package de.saumya.mojo.ruby;

import java.io.IOException;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;

public class ScriptingService {

    private final ScriptingContainer scriptingContainer;

    public ScriptingService() {
        this.scriptingContainer = new ScriptingContainer(LocalContextScope.SINGLETON,
                LocalVariableBehavior.PERSISTENT);

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

    public Object rubyObjectFromClassloader(final String name, final Class<?> clazz)
            throws IOException {
        return this.scriptingContainer.runScriptlet(ScriptUtils.getScriptAsStream(name,
                                                                                  clazz),
                                                    name);
    }

    public Object rubyObjectFromClassloader(final String name)
            throws IOException {
        return this.scriptingContainer.runScriptlet(ScriptUtils.getScriptAsStream(name),
                                                    name);
    }

    public ScriptingContainer scripting() {
        return this.scriptingContainer;
    }
}