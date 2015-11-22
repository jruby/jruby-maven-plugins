package de.saumya.mojo.mavengem;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.NoSuchFileException;

import org.sonatype.nexus.ruby.FileType;
import org.sonatype.nexus.ruby.GemArtifactFile;
import org.sonatype.nexus.ruby.IOUtil;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.cuba.RubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.Storage;

public class MavenGemURLConnection extends URLConnection {

    public static final String MAVEN_RELEASES = "/maven/releases";
    public static final String PING = MAVEN_RELEASES + "/ping";

    private InputStream in;
    private long timestamp = -1;

    // package private for testing
    final URL baseurl;
    final String path;
    final RubygemsFactory factory;

    public static MavenGemURLConnection create(String uri) throws MalformedURLException {
	return create(null, uri);
    }

    public static MavenGemURLConnection create(RubygemsFactory factory, String uri)
	     throws MalformedURLException {
	int index = uri.indexOf(MAVEN_RELEASES);
        String path = uri.substring(index);
        String baseurl = uri.substring(0, index);
	return new MavenGemURLConnection(factory, new URL(baseurl), path);
    }

    public MavenGemURLConnection(URL baseurl, String path)
	    throws MalformedURLException {
	this(null, baseurl, path);
    }

    public MavenGemURLConnection(RubygemsFactory factory, URL baseurl, String path)
	    throws MalformedURLException {
        super(baseurl);
	this.factory = factory == null ? RubygemsFactory.defaultFactory() : factory;
        this.baseurl = baseurl;
        this.path = path.startsWith(MAVEN_RELEASES) ? path : MAVEN_RELEASES + path;
    }

    @Override
    synchronized public InputStream getInputStream() throws IOException {
        if (in == null) {
            connect();
        }
        return in;
    }

    synchronized public long getModified() throws IOException {
        if (timestamp == -1) {
            connect();
        }
        return timestamp;
    }

    private int counter = 12; // seconds
    @Override
    synchronized public void connect() throws IOException {
        connect(factory.getOrCreate(baseurl));
    }

    private void connect(Rubygems facade) throws IOException {
        RubygemsFile file = facade.get(path);
        switch( file.state() )
        {
        case FORBIDDEN:
            throw new IOException("forbidden: " + file + " on " + baseurl);
        case NOT_EXISTS:
            if (path.equals(PING)) {
                in = new ByteArrayInputStream("pong".getBytes());
                break;
            }
	    throw new FileNotFoundException(file.toString() + " on " + baseurl);
        case NO_PAYLOAD:
            switch( file.type() )
            {
            case GEM_ARTIFACT:
                // we can pass in null as dependenciesData since we have already the gem
                in = new URL(baseurl + "/gems/" + ((GemArtifactFile) file ).gem( null ).filename() + ".gem" ).openStream();
            case GEM:
		// TODO timestamp
                in = new URL(baseurl + "/" +  file.remotePath()).openStream();
            default:
                throw new FileNotFoundException("view - not implemented. " + file + " on " + baseurl + " on " + baseurl);
            }
        case ERROR:
	    if (file.getException() instanceof NoSuchFileException) {
		throw new FileNotFoundException(file.toString() + " on " + baseurl);
	    }
	    throw new IOException(file.toString() + " on " + baseurl, file.getException());
        case TEMP_UNAVAILABLE:
            try {
                Thread.currentThread().sleep(1000);
            }
            catch(InterruptedException ignore) {
            }
            if (--counter > 0) {
                connect(facade);
            }
            break;
        case PAYLOAD:
            in = facade.getInputStream(file);
	    timestamp = facade.getModified(file);
            break;
        case NEW_INSTANCE:
        default:
            throw new RuntimeException("BUG: should never reach here. " + file + " on " + baseurl);
        }
    }
}

