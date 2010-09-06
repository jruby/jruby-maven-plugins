/**
 * 
 */
package de.saumya.mojo.ruby;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;

public class GemScriptingContainer extends ScriptingContainer {

    public GemScriptingContainer(final File gemHome, final File gemPath) {
        super(LocalContextScope.SINGLETON, LocalVariableBehavior.PERSISTENT);
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
        return runScriptlet(ScriptUtils.getScriptAsStream(name, clazz), name);
    }

    public Object runScriptletFromClassloader(final String name)
            throws IOException {
        return runScriptlet(ScriptUtils.getScriptAsStream(name), name);
    }

}