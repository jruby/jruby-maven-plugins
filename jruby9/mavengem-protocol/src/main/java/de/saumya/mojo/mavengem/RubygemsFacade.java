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

public class RubygemsFacade {

    private static RubygemsGateway gateway = new DefaultRubygemsGateway(new IsolatedScriptingContainer());

    private static Map<URL, RubygemsFacade> facades = new HashMap<URL, RubygemsFacade>();

    static synchronized RubygemsFacade getOrCreate(URL url)
            throws MalformedURLException {
	if (catchAllMirror != null) {
	    url = catchAllMirror;
	}
	else {
	    String key = "mavengem.mirror";
	    if (System.getProperty(key) != null) {
		url = new URL(System.getProperty(key));
	    }
	    else {
		if (mirrors != null && mirrors.containsKey(url)) {
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

        RubygemsFacade result = facades.get(url);
        if (result == null) {
	    result = new RubygemsFacade(url, cacheDir());
            facades.put(url, result);
        }
        return result;
    }

    static File cacheDir = null;
    static File cacheDir() {
	if (cacheDir != null) {
	    return cacheDir;
	}
	if (System.getProperty("mavengem.home") != null) {
	    return new File(System.getProperty("mavengem.home"));
	}
	return new File(System.getProperty("user.home"), ".mavengem");
    }

    static void setCacheDir(File dir) {
	cacheDir = dir;
    }

    static Map<URL, URL> mirrors = null;
    static void setMirrors(Map<URL, URL> mirrors) {
	mirrors = mirrors;
    }

    static URL catchAllMirror = null;
    static void setCatchAllMirror(URL mirror) {
	catchAllMirror = mirror;
    }

    private final ProxyStorage storage;
    private final RubygemsFileSystem files;

    RubygemsFacade(URL url, File baseCacheDir) {
	// we do not want to expose credentials inside the directory name
        File cachedir = new File(baseCacheDir, url.toString().replaceFirst("://[^:]+:[^:]+@", "://").replaceAll("[/:.]", "_"));
	this.storage = new CachingProxyStorage(cachedir, url);
        this.files = new ProxiedRubygemsFileSystem(gateway, storage);
    }

    public InputStream getInputStream(RubygemsFile file) throws IOException {
        return this.storage.getInputStream(file);
    }

    public RubygemsFile get(String path) {
        return this.files.get(path);
    }

    public long getModified(RubygemsFile file) {
	return this.storage.getModified(file);
    }
}

