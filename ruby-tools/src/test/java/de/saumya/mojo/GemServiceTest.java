package de.saumya.mojo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.codehaus.classworlds.ClassWorld;
import org.xml.sax.helpers.XMLReaderAdapter;

public class GemServiceTest extends TestCase {

    private static class NoLog implements Log {

        // public void debug(final CharSequence content) {
        // }
        //
        // public void debug(final Throwable error) {
        // }
        //
        // public void debug(final CharSequence content, final Throwable error)
        // {
        // }
        //
        // public void error(final CharSequence content) {
        // }
        //
        // public void error(final Throwable error) {
        // }
        //
        // public void error(final CharSequence content, final Throwable error)
        // {
        // }

        public void info(final CharSequence content) {
        }

        // public void info(final Throwable error) {
        // }
        //
        // public void info(final CharSequence content, final Throwable error) {
        // }
        //
        // public boolean isDebugEnabled() {
        // return false;
        // }
        //
        // public boolean isErrorEnabled() {
        // return false;
        // }
        //
        // public boolean isInfoEnabled() {
        // return false;
        // }
        //
        // public boolean isWarnEnabled() {
        // return false;
        // }
        //
        // public void warn(final CharSequence content) {
        // }
        //
        // public void warn(final Throwable error) {
        // }
        //
        // public void warn(final CharSequence content, final Throwable error) {
        // }

    }

    public GemServiceTest(final String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(GemServiceTest.class);
    }

    GemService          gemspec;
    private static long time;

    @Override
    public void setUp() throws Exception {
        final LauncherFactory factory = new LauncherFactory();
        final List<String> classpathElements = new ArrayList<String>();
        classpathElements.add(".");
        this.gemspec = new GemService(new NoLog(),
                factory.getEmbeddedLauncher(true,
                                            classpathElements,
                                            new HashMap<String, String>(),
                                            new File("./pom.xml"),
                                            new ClassWorld().newRealm("test")
                                                    .createChildRealm("child")));

    }

    public void testGemspec() throws Exception {
        final long start = System.currentTimeMillis();
        this.gemspec.convertGemspec2Pom(new File("target/test-classes/test.gemspec"),
                                        new File("target/pom.xml"));
        // assume parsing it should be test enough here !!
        new XMLReaderAdapter().parse("target/pom.xml");
        final long end = System.currentTimeMillis();
        time = end - start;
    }

    public void testGemspecAgain() throws Exception {
        final long oldTime = time;
        testGemspec();
        assertTrue(time + " < " + (oldTime), time < oldTime / 2);
    }

    public void testUpdateMetadata() throws Exception {
        final File repo = new File(System.getProperty("user.home")
                + "/.m2/repository/rubygems");
        cleanupMetadata("rubygems-test", repo);
        final List<String> remoteRepos = new ArrayList<String>();
        remoteRepos.add("rubygems-test");
        this.gemspec.updateMetadata(remoteRepos, repo.getAbsolutePath());

        assertTrue(cleanupMetadata("rubygems-test", repo) > 0);
    }

    private int cleanupMetadata(final String repoId, final File repo) {
        int count = 0;
        for (final File dir : repo.listFiles()) {
            if (dir.isDirectory()) {
                for (final File f : dir.listFiles()) {
                    if (f.getName().startsWith("maven-metadata-" + repoId)) {
                        f.delete();
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
