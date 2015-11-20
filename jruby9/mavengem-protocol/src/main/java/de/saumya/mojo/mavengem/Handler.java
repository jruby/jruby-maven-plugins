package de.saumya.mojo.mavengem;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {

    public static String KEY = "java.protocol.handler.pkgs";
    public static String PKG = "de.saumya.mojo";
    public static String PING_URL = "mavengem:https://rubygems.org" + MavenGemURLConnection.PING;

    private static RubygemsFacadeFactory factory;

    public synchronized static boolean isMavenGemProtocolRegistered() {
        return System.getProperties().contains(KEY);
    }

    public synchronized static boolean registerMavenGemProtocol() {
	if (ping()) {
	    // we can register the protocol only once
	    return false;
	}
	factory = new RubygemsFacadeFactory();

	return doRegisterMavenGemProtocol();
    }

    public synchronized static boolean registerMavenGemProtocol(File cacheDir) {
	if (ping()) {
	    // we can register the protocol only once
	    return false;
	}
	factory = new RubygemsFacadeFactory(cacheDir);

	return doRegisterMavenGemProtocol();
    }

    public synchronized static boolean registerMavenGemProtocol(File cacheDir, Map<URL,URL> mirrors) {
	if (ping()) {
	    // we can register the protocol only once
	    return false;
	}
	factory = new RubygemsFacadeFactory(cacheDir, mirrors);

	return doRegisterMavenGemProtocol();
    }

    public synchronized static boolean registerMavenGemProtocol(File cacheDir, URL catchAllMirror) {
	if (ping()) {
	    // we can register the protocol only once
	    return false;
	}
	factory = new RubygemsFacadeFactory(cacheDir, catchAllMirror);

	return doRegisterMavenGemProtocol();
    }

    private static boolean doRegisterMavenGemProtocol() {
        if (System.getProperties().contains(KEY)) {
            String current = System.getProperty(KEY);
            if (!current.contains(PKG)) {
                System.setProperty(KEY, current + "|" + PKG);
            }
        }
        else {
            System.setProperty(KEY, PKG);
        }
	return ping();
    }

    private static boolean ping() {
	try {
	    // this url works offline as /maven/releases/ping is
	    // not a remote resource. but using the protocol here
	    // will register this instance Handler and other
	    // classloaders will be able to use the mavengem-protocol as well
	    // this does not go online see MavenGemURLConnection
	    URL url = new URL(PING_URL);
	    try (InputStream in = url.openStream()) {
		byte[] data = new byte[in.available()];
		in.read(data, 0, data.length);
		return "pong".equals(new String(data));
	    }
        }
        catch(IOException e) {
            return false;
        }
    }

    private String uri;

    @Override
    protected void parseURL(URL u, String spec, int start, int end) {
        uri = spec.substring(start, end);
        super.parseURL(u, spec, start, end);
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return MavenGemURLConnection.create(factory, uri);
    }
}
