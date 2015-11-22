package de.saumya.mojo.mavengem;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.net.URL;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;

public class HandlerTest {

    private boolean result;
    private File cacheDir;
    
    @Before
    public void setup() throws Exception {
	cacheDir = new File(System.getProperty("basedir"), "target/cache4test");
	result = Handler.registerMavenGemProtocol(new RubygemsFactory(cacheDir));
    }

    @Test
    public void registerProtocol() throws Exception {
	assertThat(result, is(true));
	assertThat(Handler.registerMavenGemProtocol(null),
		   is(false));
	assertThat(Handler.registerMavenGemProtocol(), is(false));
    }

    @Test
    public void virtusPomWithAuthentication() throws Exception {
	// this test goes online to rubygems.org
	File cached = new File(cacheDir, "http___rubygems_org/quick/Marshal.4.8/v/virtus-1.0.5.gemspec.rz");
	cached.delete();
	URL url = new URL("mavengem:http://me:andthecorner@rubygems.org/maven/releases/rubygems/virtus/1.0.5/virtus-1.0.5.pom");
	try (InputStream in = url.openStream()) {
	    // just read the file
	    byte[] data = new byte[in.available()];
	    in.read(data, 0, in.available());
	}
	// the cached dir does not expose the credentials
	assertThat(cached.getPath(), cached.exists(), is(true));
    }

    @Test
    public void ping() throws Exception {
	URL url = new URL("mavengem:https://rubygems.org/something/maven/releases/ping");
	byte[] data = new byte[4];
	url.openStream().read(data, 0, 4);
	String result = new String(data);
	assertThat(result, is("pong"));
    }

    @Test
    public void railsMavenMetadata() throws Exception {
	// this test goes online to rubygems.org
	File cached = new File(cacheDir, "https___rubygems_org/api/v1/dependencies/rails.ruby");
	cached.delete();
	URL url = new URL("mavengem:https://rubygems.org/maven/releases/rubygems/rails/maven-metadata.xml");
	try (InputStream in = url.openStream()) {
	    byte[] data = new byte[in.available()];
	    in.read(data, 0, in.available());
	    String result = new String(data);
	    assertThat(result, startsWith("<metadata>"));
	    assertThat(result, containsString("<version>4.2.5</version>"));
	    assertThat(result, endsWith("</metadata>\n"));
	}
	assertThat(cached.getPath(), cached.isFile(), is(true));
    }

    @Test
    public void railsPom() throws Exception {
	// this test goes online to rubygems.org
	File cached = new File(cacheDir, "https___rubygems_org/quick/Marshal.4.8/r/rails-4.2.5.gemspec.rz");
	cached.delete();
	URL url = new URL("mavengem:https://rubygems.org/maven/releases/rubygems/rails/4.2.5/rails-4.2.5.pom");
	try (InputStream in = url.openStream()) {
	    byte[] data = new byte[in.available()];
	    in.read(data, 0, in.available());
	    String result = new String(data);
	    assertThat(result, startsWith("<project>"));
	    assertThat(result, containsString("<version>4.2.5</version>"));
	    assertThat(result, containsString("<name>Full-stack web application framework.</name>"));
	    assertThat(result, containsString("<packaging>gem</packaging>"));
	    assertThat(result, endsWith("</project>\n"));
	}
	assertThat(cached.getPath(), cached.isFile(), is(true));
    }

    @Test(expected = FileNotFoundException.class) 
    public void fileNotFoundOnWrongBaseURL() throws Exception {
	// this test goes online to rubygems.org
	URL url = new URL("mavengem:https://rubygems.org/something/not/right/here/maven/releases/rubygems/rails/maven-metadata.xml");
	url.openStream();
    }

    @Test(expected = FileNotFoundException.class) 
    public void fileNotFoundOnDirectory() throws Exception {
	// this test goes online to rubygems.org
	URL url = new URL("mavengem:https://rubygems.org/maven/releases/rubygems/rails");
	url.openStream();
    }
}
