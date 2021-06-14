package de.saumya.mojo.ruby.script;

import de.saumya.mojo.ruby.NoopLogger;
import de.saumya.mojo.ruby.gems.GemsConfig;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;

public class TestDataFactories {

    public static GemScriptFactory gemScriptFactory() throws ScriptException, IOException {
        return new GemScriptFactory(
                new NoopLogger(),
                null,
                findJRubyJar(),
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

    public static File findJRubyJar() {
        return findForClass(org.jruby.Main.class);
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
}
