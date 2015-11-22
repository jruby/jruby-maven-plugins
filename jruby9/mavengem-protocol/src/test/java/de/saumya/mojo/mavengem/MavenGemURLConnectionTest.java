package de.saumya.mojo.mavengem;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;

public class MavenGemURLConnectionTest {

    private File cacheDir;
    private RubygemsFactory factory;

    @Before
    public void setup() throws Exception {
	cacheDir = new File(System.getProperty("basedir"), "target/cache4test");
	factory = new RubygemsFactory(cacheDir);
    }

    @Test
    public void virtusPomWithAuthentication() throws Exception {
	// this test goes online to rubygems.org
	File cached = new File(cacheDir, "http___rubygems_org/quick/Marshal.4.8/v/virtus-1.0.5.gemspec.rz");
	
	// with the /maven/releases prefix
	URLConnection url = new MavenGemURLConnection(factory, new URL("http://me:andthecorner@rubygems.org"), "/maven/releases/rubygems/virtus/1.0.5/virtus-1.0.5.pom");
	
	// the cached dir does not expose the credentials
	assertCached(url, cached);

	// without the /maven/releases prefix
	url = new MavenGemURLConnection(factory, new URL("http://me:andthecorner@rubygems.org"), "/rubygems/virtus/1.0.5/virtus-1.0.5.pom");
	
	// the cached dir does not expose the credentials
	assertCached(url, cached);
    }

    @Test
    public void virtusPomWithMirror() throws Exception {
	// this test goes online to rubygems.org
	File cached = new File(cacheDir, "http___rubygems_org/quick/Marshal.4.8/v/virtus-1.0.5.gemspec.rz");
	RubygemsFactory factory = new RubygemsFactory(cacheDir, new URL("http://me:andthecorner@rubygems.org"));
	
	// with the /maven/releases prefix
	URLConnection url = new MavenGemURLConnection(factory, new URL("http://example.com"), "/maven/releases/rubygems/virtus/1.0.5/virtus-1.0.5.pom");

	// the cached dir does not expose the credentials
	assertCached(url, cached);

	// without the /maven/releases prefix
	url = new MavenGemURLConnection(factory, new URL("http://example.com"), "/rubygems/virtus/1.0.5/virtus-1.0.5.pom");

	// the cached dir does not expose the credentials
	assertCached(url, cached);
    }

    @Test
    public void virtusPomWithMirrors() throws Exception {
	// this test goes online to rubygems.org
	File cached = new File(cacheDir, "http___rubygems_org/quick/Marshal.4.8/v/virtus-1.0.5.gemspec.rz");
	Map<URL,URL> mirrors = new HashMap<URL,URL>();
	mirrors.put(new URL("http://example.com"), new URL("http://me:andthecorner@rubygems.org"));
	mirrors.put(new URL("http://hans:glueck@example.org"), new URL("http://rubygems.org"));
	RubygemsFactory factory = new RubygemsFactory(cacheDir, mirrors);
	
	// with the /maven/releases prefix
	URLConnection url = new MavenGemURLConnection(factory, new URL("http://example.com"), "/maven/releases/rubygems/virtus/1.0.5/virtus-1.0.5.pom");

	// the cached dir does not expose the credentials
	assertCached(url, cached);

	// without the /maven/releases prefix
	url = new MavenGemURLConnection(factory, new URL("http://example.org"), "/rubygems/virtus/1.0.5/virtus-1.0.5.pom");
	assertCached(url, cached);

	// go direct here
	cached = new File(cacheDir, "https___rubygems_org/quick/Marshal.4.8/v/virtus-1.0.5.gemspec.rz");
	url = new MavenGemURLConnection(factory, new URL("https://rubygems.org"), "/rubygems/virtus/1.0.5/virtus-1.0.5.pom");
	assertCached(url, cached);
    }

    private void assertCached(URLConnection url, File cached) throws Exception {
	cached.delete();
	try (InputStream in = url.getInputStream()) {
	    // just read the file
	    byte[] data = new byte[in.available()];
	    in.read(data, 0, in.available());
	}
	// the cached dir does not expose the credentials
	assertThat(cached.getPath(), cached.exists(), is(true));
    }

    @Test
    public void ping() throws Exception {
	URLConnection url = new MavenGemURLConnection(new URL("https://rubygems.org/something"), "/maven/releases/ping");
	byte[] data = new byte[4];
	url.getInputStream().read(data, 0, 4);
	String result = new String(data);
	assertThat(result, is("pong"));
    }

    @Test
    public void railsMavenMetadata() throws Exception {
	// this test goes online to rubygems.org
	File cached = new File(cacheDir, "https___rubygems_org/api/v1/dependencies/rails.ruby");
	cached.delete();
	URLConnection url = new MavenGemURLConnection(factory, new URL("https://rubygems.org"), "/maven/releases/rubygems/rails/maven-metadata.xml");
	String result = download(url);

	assertThat(result, startsWith("<metadata>"));
	assertThat(result, containsString("<version>4.2.5</version>"));
	assertThat(result, endsWith("</metadata>\n"));
	assertThat(cached.getPath(), cached.isFile(), is(true));
    }

    String download(URLConnection url) throws Exception {
	try (InputStream in = url.getInputStream()) {
	    byte[] data = new byte[in.available()];
	    in.read(data, 0, in.available());
	    return new String(data);
	}
    }

    @Test
    public void railsPom() throws Exception {
	// this test goes online to rubygems.org
	File cached = new File(cacheDir, "https___rubygems_org/quick/Marshal.4.8/r/rails-4.2.5.gemspec.rz");
	cached.delete();
	URLConnection url = new MavenGemURLConnection(factory, new URL("https://rubygems.org"), "/rubygems/rails/4.2.5/rails-4.2.5.pom");
	String result = download(url);

	assertThat(result, startsWith("<project>"));
	assertThat(result, containsString("<version>4.2.5</version>"));
	assertThat(result, containsString("<name>Full-stack web application framework.</name>"));
	assertThat(result, containsString("<packaging>gem</packaging>"));
	assertThat(result, endsWith("</project>\n"));
	assertThat(cached.getPath(), cached.isFile(), is(true));
    }

    @Test(expected = FileNotFoundException.class)
    public void fileNotFoundOnWrongBaseURL() throws Exception {
	// this test goes online to rubygems.org
	URLConnection url = new MavenGemURLConnection(factory, new URL("https://rubygems.org/something/not/right/here"), "/maven/releases/rubygems/rails/maven-metadata.xml");
	url.getInputStream();
    }

    @Test(expected = FileNotFoundException.class)
    public void fileNotFoundOnDirectory() throws Exception {
	// this test goes online to rubygems.org
	URLConnection url = new MavenGemURLConnection(factory, new URL("https://rubygems.org"), "/maven/releases/rubygems/rails");
	url.getInputStream();
    }
}
