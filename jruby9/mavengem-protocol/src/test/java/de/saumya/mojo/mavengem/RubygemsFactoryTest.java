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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;

public class RubygemsFactoryTest {

    @Test
    public void defaultsOnDefaultFactory() throws Exception {
	RubygemsFactory.factory = null;
	System.getProperties().remove("mavengem.cachedir");
	System.getProperties().remove("mavengem.mirror");
	RubygemsFactory factory = RubygemsFactory.defaultFactory();

	assertThat(factory.mirrors, nullValue());
	assertThat(factory.catchAllMirror, is(RubygemsFactory.NO_MIRROR));
	assertThat(factory.cacheDir.getName(), is(".mavengem"));
    }

    @Test
    public void defaultFactory() throws Exception {
	System.setProperty("mavengem.cachedir", "some/thing");
	System.setProperty("mavengem.mirror", "https://example.org");
	RubygemsFactory.factory = null;
	RubygemsFactory factory = RubygemsFactory.defaultFactory();

	assertThat(factory.mirrors, nullValue());
	assertThat(factory.catchAllMirror, is(new URL("https://example.org")));
	assertThat(factory.cacheDir, is(new File("some/thing")));
    }
    
    @Test
    public void defaultsOnInstance() throws Exception {
	RubygemsFactory factory = new RubygemsFactory();

	assertThat(factory.mirrors, nullValue());
	assertThat(factory.catchAllMirror, is(RubygemsFactory.NO_MIRROR));
	assertThat(factory.cacheDir.getName(), is(".mavengem"));
    }

    @Test
    public void cachedir() throws Exception {
	RubygemsFactory factory = new RubygemsFactory(new File("some/thing"));

	assertThat(factory.mirrors, nullValue());
	assertThat(factory.catchAllMirror, is(RubygemsFactory.NO_MIRROR));
	assertThat(factory.cacheDir, is(new File("some/thing")));
    }

    @Test
    public void mirror() throws Exception {
	RubygemsFactory factory = new RubygemsFactory(new URL("https://example.org"));
	
	assertThat(factory.mirrors, nullValue());
	assertThat(factory.catchAllMirror, is(new URL("https://example.org")));
	assertThat(factory.cacheDir.getName(), is(".mavengem"));
    }

    @Test
    public void mirrors() throws Exception {
	Map<URL,URL> mirrors = new HashMap<URL,URL>();
	RubygemsFactory factory = new RubygemsFactory(mirrors);
	
	assertThat(factory.mirrors, is(mirrors));
	assertThat(factory.catchAllMirror, is(RubygemsFactory.NO_MIRROR));
	assertThat(factory.cacheDir.getName(), is(".mavengem"));
    }

    @Test
    public void cachedirAndMirror() throws Exception {
	RubygemsFactory factory = new RubygemsFactory(new File("some/thing"),
						      new URL("https://example.org"));

	assertThat(factory.mirrors, nullValue());
	assertThat(factory.catchAllMirror, is(new URL("https://example.org")));
	assertThat(factory.cacheDir, is(new File("some/thing")));
    }

    @Test
    public void cachedirAndMirrors() throws Exception {
	Map<URL,URL> mirrors = new HashMap<URL,URL>();
	RubygemsFactory factory = new RubygemsFactory(new File("some/thing"), mirrors);
	
	assertThat(factory.mirrors, is(mirrors));
	assertThat(factory.catchAllMirror, is(RubygemsFactory.NO_MIRROR));
	assertThat(factory.cacheDir, is(new File("some/thing")));
    }
}
