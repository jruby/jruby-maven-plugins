package de.saumya.mojo.ruby.script;

import de.saumya.mojo.ruby.NoopLogger;
import de.saumya.mojo.ruby.gems.GemsConfig;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;

public class TestDataFactories {

    public static GemScriptFactory gemScriptFactory(ClassLoader cl) throws ScriptException, IOException {
        return new GemScriptFactory(
                new NoopLogger(),
                null,
                findJRubyJar(cl),
                new ArrayList<String>(),
                true,
                gemsConfig());
    }

    public static GemsConfig gemsConfig() {
        final GemsConfig config = new GemsConfig();
        config.setGemHome(new File("target"));
        config.addGemPath(new File("target/rubygems"));
        return config;
    }

    public static File findJRubyJar(ClassLoader cl) {
        final File inClassLoader = findInClassLoader((URLClassLoader) cl);
        if (inClassLoader == null) {
            return findForClass(org.jruby.Main.class);
        } else {
            return inClassLoader;
        }
    }

    private static File findForClass(Class<?> className) {
        final String fullClassname = '/' + className.getName().replace('.', '/') + ".class";
        final URL location = className.getResource(fullClassname);
        if (location != null) {
            String substring = urlAsAbsolutePath(location, fullClassname);
            return new File(substring);
        }
        return null;
    }

    private static String urlAsAbsolutePath(URL url, String fullClassname) {
        final String path = url.getPath();
        if (path.startsWith("file:"))
            return path.substring("file:".length(), path.indexOf("!" + fullClassname));
        else
            return path;
    }

    private static File findInClassLoader(URLClassLoader cl) {
        URLClassLoader classLoader = cl;
        for (URL url : classLoader.getURLs()) {
            if (url.getFile().contains("jruby-complete")) {
                return new File(url.getPath());
            }
            if (url.getFile().contains("jruby-core")) {
                return new File(url.getPath());
            }
        }
        return null;
    }
}
