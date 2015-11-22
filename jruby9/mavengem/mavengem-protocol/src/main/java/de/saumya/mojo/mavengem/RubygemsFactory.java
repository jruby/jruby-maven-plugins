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

public class RubygemsFactory {

    private static RubygemsGateway gateway = new DefaultRubygemsGateway(new IsolatedScriptingContainer());

    private static Map<URL, Rubygems> facades = new HashMap<URL, Rubygems>();
    static URL NO_MIRROR;
    static {
	try {
	    NO_MIRROR = new URL("http://example.com/no_mirror");
	}
	catch (MalformedURLException e) {
	    throw new RuntimeException( "can not happen", e);
	}
    }
    public static File DEFAULT_CACHEDIR = new File(System.getProperty("user.home"), ".mavengem");

    public static final String MAVENGEM_MIRROR = "mavengem.mirror";
    public static final String MAVENGEM_CACHEDIR = "mavengem.cachedir";

    // keep package access for testing
    final File cacheDir;
    final Map<URL, URL> mirrors;
    final URL catchAllMirror;

    static RubygemsFactory factory;
    public static synchronized RubygemsFactory defaultFactory()
	    throws MalformedURLException {
	if (factory == null) {
	    factory = new RubygemsFactory(null, null, null, false);
	}
	return factory;
    }

    public RubygemsFactory()
	    throws MalformedURLException {
	this(DEFAULT_CACHEDIR, NO_MIRROR, null);
    }

    public RubygemsFactory(URL mirror)
	    throws MalformedURLException {
	this(DEFAULT_CACHEDIR, mirror, null);
    }

    public RubygemsFactory(Map<URL, URL> mirrors)
	    throws MalformedURLException {
	this(DEFAULT_CACHEDIR, NO_MIRROR, mirrors);
    }

    public RubygemsFactory(File cacheDir)
	    throws MalformedURLException {
	this(cacheDir, NO_MIRROR, null);
    }

    public RubygemsFactory(File cacheDir, URL mirror)
	    throws MalformedURLException {
	this(cacheDir, mirror, null);
    }

    public RubygemsFactory(File cacheDir, Map<URL, URL> mirrors)
	    throws MalformedURLException {
	this(cacheDir, NO_MIRROR, mirrors);
    }

    private RubygemsFactory(File cacheDir, URL mirror, Map<URL, URL> mirrors)
	    throws MalformedURLException {
	this(cacheDir, mirror, mirrors, true);
    }

    private RubygemsFactory(File cacheDir, URL mirror, Map<URL, URL> mirrors, boolean check)
	    throws MalformedURLException {
	if (check) {
	    if (cacheDir == null) {
		throw new IllegalArgumentException("cache directory can not be null");
	    }
	    if (mirror == null) {
		throw new IllegalArgumentException("mirror can not be null");
	    }
	}
	if (mirror != null) {
	    this.catchAllMirror = mirror;
	}
	else {
	    if (System.getProperty(MAVENGEM_MIRROR) != null) {
		this.catchAllMirror = new URL(System.getProperty(MAVENGEM_MIRROR));
	    }
	    else {
		this.catchAllMirror = NO_MIRROR;
	    }
	}
	this.mirrors = mirrors == null ? null : new HashMap(mirrors);
	if (cacheDir != null) {
	    this.cacheDir = cacheDir;
	}
	else if (System.getProperty(MAVENGEM_CACHEDIR) != null) {
	    this.cacheDir = new File(System.getProperty(MAVENGEM_CACHEDIR));
	}
	else {
	    this.cacheDir = DEFAULT_CACHEDIR;
	}
    }

    public Rubygems getOrCreate(URL url)
            throws MalformedURLException {
	if (this.catchAllMirror != NO_MIRROR) {
	    url = this.catchAllMirror;
	}
	else if (this.mirrors != null && this.mirrors.containsKey(url)) {
	    url = mirrors.get(url);
	}

	// FIXME the cachedir when coming from the facades map
	//       can be different as map get shared between factories
	synchronized(facades) {
	    Rubygems result = facades.get(url);
	    if (result == null) {
		result = new Rubygems(url, this.cacheDir);
		facades.put(url, result);
	    }
	    return result;
	}
    }
}

