package de.saumya.mojo.ruby;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.xml.sax.helpers.XMLReaderAdapter;

import de.saumya.mojo.gems.GemspecConverter;

public class GemServiceTest extends TestCase {

    private static class NoLog implements Log {
        public void info(final CharSequence content) {
        }
    }

    public GemServiceTest(final String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(GemServiceTest.class);
    }

    GemspecConverter    gemspec;
    private static long time;

    @Override
    public void setUp() throws Exception {
        final LauncherFactory factory = new EmbeddedLauncherFactory();
        final List<String> classpathElements = new ArrayList<String>();
        classpathElements.add(".");
        final Map<String, String> env = new HashMap<String, String>();
        // setup local rubygems repository
        final File rubygems = new File("target/rubygems");
        rubygems.mkdirs();
        env.put("GEM_PATH", rubygems.getAbsolutePath());
        env.put("GEM_HOME", rubygems.getAbsolutePath());
        this.gemspec = new GemspecConverter(new NoLog(),
                factory.getLauncher(true,
                                    classpathElements,
                                    env,
                                    new File("./pom.xml"),
                                    null));

    }

    public void testGemspec() throws Exception {
        final long start = System.currentTimeMillis();
        this.gemspec.createPom(new File("target/test-classes/test.gemspec"),
                               "0.20.0",
                               new File("target/pom.xml"));
        // assume parsing the pom is test enough here !!
        new XMLReaderAdapter().parse("target/pom.xml");
        final long end = System.currentTimeMillis();
        time = end - start;
    }

    public void testGemspecAgain() throws Exception {
        final long oldTime = time;
        testGemspec();
        assertTrue(time + " < " + (oldTime), time < oldTime);
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
