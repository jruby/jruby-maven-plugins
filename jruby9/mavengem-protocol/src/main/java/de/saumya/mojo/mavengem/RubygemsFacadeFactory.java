package de.saumya.mojo.mavengem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jruby.embed.IsolatedScriptingContainer;
import org.jruby.embed.ScriptingContainer;
import org.sonatype.nexus.ruby.DefaultRubygemsGateway;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.FileType;
import org.sonatype.nexus.ruby.GemArtifactFile;
import org.sonatype.nexus.ruby.IOUtil;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.cuba.RubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.CachingProxyStorage;
import org.sonatype.nexus.ruby.layout.ProxiedRubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.ProxyStorage;

public class RubygemsFacadeFactory {

    private static RubygemsGateway gateway = new DefaultRubygemsGateway(new IsolatedScriptingContainer());

    private static Map<URL, RubygemsFacade> facades = new HashMap<URL, RubygemsFacade>();

    private final File cacheDir;
    private final Map<URL, URL> mirrors;
    private final URL catchAllMirror;

    private static RubygemsFacadeFactory factory;
    public static synchronized RubygemsFacadeFactory defaultFactory() {
	if (factory == null) {
	    factory = new RubygemsFacadeFactory();
	}
	return factory;
    }

    public RubygemsFacadeFactory() {
	this(null, null, null);
    }

    public RubygemsFacadeFactory(File cacheDir) {
	this(cacheDir, null, null);
    }

    public RubygemsFacadeFactory(File cacheDir, URL mirror) {
	this(cacheDir, mirror, null);
    }

    public RubygemsFacadeFactory(File cacheDir, Map<URL, URL> mirrors) {
	this(cacheDir, null, mirrors);
    }

    private RubygemsFacadeFactory(File cacheDir, URL mirror, Map<URL, URL> mirrors) {
	this.catchAllMirror = mirror;
	this.mirrors = mirrors == null ? null : new HashMap(mirrors);
	if (cacheDir != null) {
	    this.cacheDir = cacheDir;
	}
	else if (System.getProperty("mavengem.home") != null) {
	    this.cacheDir = new File(System.getProperty("mavengem.home"));
	}
	else {
	    this.cacheDir = new File(System.getProperty("user.home"), ".mavengem");
	}
    }

    public RubygemsFacade getOrCreate(URL url)
            throws MalformedURLException {
	if (this.catchAllMirror != null) {
	    url = this.catchAllMirror;
	}
	else {
	    String key = "mavengem.mirror";
	    if (System.getProperty(key) != null) {
		url = new URL(System.getProperty(key));
	    }
	    else {
		if (this.mirrors != null && this.mirrors.containsKey(url)) {
		    url = mirrors.get(url);
		}
		else {
		    key = "mavengem.mirror." + url.toString();
		    if (System.getProperty(key) != null) {
			url = new URL(System.getProperty(key));
		    }
		}
	    }
	}

	// FIXME the cachedir when coming from the facades map
	//       can be different as map get shared between factories
	synchronized(facades) {
	    RubygemsFacade result = facades.get(url);
	    if (result == null) {
		result = new RubygemsFacade(url, this.cacheDir);
		facades.put(url, result);
	    }
	    return result;
	}
    }
}

