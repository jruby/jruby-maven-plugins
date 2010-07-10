package de.saumya.mojo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.codehaus.classworlds.ClassWorld;
import org.xml.sax.helpers.XMLReaderAdapter;

public class GemspecTest extends TestCase {

    public GemspecTest(final String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(GemspecTest.class);
    }

    GemspecService      gemspec;
    private static long time;

    @Override
    public void setUp() throws Exception {
        final LauncherFactory factory = new LauncherFactory();
        final List<String> classpathElements = new ArrayList<String>();
        classpathElements.add(".");
        this.gemspec = new GemspecService(factory.getEmbeddedLauncher(true,
                                                                      classpathElements,
                                                                      null,
                                                                      null,
                                                                      new File("./pom.xml"),
                                                                      new ClassWorld().newRealm("test")
                                                                              .createChildRealm("child")),
                new File(new File(System.getProperty("user.home")), ".m2"));

    }

    public void test() throws Exception {
        final long start = System.currentTimeMillis();
        this.gemspec.convertGemspec2Pom(new File("target/test-classes/test.gemspec"),
                                        new File("target/pom.xml"));
        // assume parsing it should be test enough here !!
        new XMLReaderAdapter().parse("target/pom.xml");
        final long end = System.currentTimeMillis();
        time = end - start;
    }

    public void testAgain() throws Exception {
        final long oldTime = time;
        test();
        assertTrue(time + " < " + (oldTime), time < oldTime / 2);
    }
}
