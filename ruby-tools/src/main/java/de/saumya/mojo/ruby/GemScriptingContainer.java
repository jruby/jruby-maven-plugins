/**
 *
 */
package de.saumya.mojo.ruby;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;

public class GemScriptingContainer extends ScriptingContainer {

    public GemScriptingContainer(final File gemHome, final File gemPath) {
        this(LocalContextScope.SINGLETON,
                LocalVariableBehavior.PERSISTENT,
                gemHome,
                gemPath);
    }

    public GemScriptingContainer(LocalContextScope scope,
            LocalVariableBehavior behavior) {
        this(scope, behavior, null, null);
    }

    public GemScriptingContainer(LocalContextScope scope,
            LocalVariableBehavior behavior, final File gemHome,
            final File gemPath) {
        super(scope, behavior);
        final Map<String, String> env = new HashMap<String, String>();
        if (gemHome != null && gemHome.exists()) {
            env.put("GEM_HOME", gemHome.getAbsolutePath());
        }
        if (gemPath != null && gemPath.exists()) {
            env.put("GEM_PATH", gemPath.getAbsolutePath());
        }
        getProvider().getRubyInstanceConfig().setEnvironment(env);

        // setting the JRUBY_HOME to the one from the jruby jar - ignoring
        // the environment setting !
        getProvider().getRubyInstanceConfig()
                .setJRubyHome(Thread.currentThread()
                        .getContextClassLoader()
                        .getResource("META-INF/jruby.home")
                        .toString()
                        .replaceFirst("^jar:", ""));
    }

    public Object runScriptletFromClassloader(final String name,
            final Class<?> clazz) throws IOException {
        InputStream script = ScriptUtils.getScriptAsStream(name, clazz);
        try {
            return runScriptlet(script, name);
        }
        finally {
            if (script != null) {
                script.close();
            }
        }
    }

    public Object runScriptletFromClassloader(final String name)
            throws IOException {
        InputStream script = ScriptUtils.getScriptAsStream(name);
        try {
            return runScriptlet(ScriptUtils.getScriptAsStream(name), name);
        }
        finally {
            if (script != null) {
                script.close();
            }
        }
    }

}