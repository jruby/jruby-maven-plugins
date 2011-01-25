package de.saumya.mojo.ruby;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.xml.sax.helpers.XMLReaderAdapter;

import de.saumya.mojo.gems.GemspecConverter;
import de.saumya.mojo.ruby.script.GemScriptFactory;
import de.saumya.mojo.ruby.script.ScriptFactory;

public class GemServiceTest extends TestCase {

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
        final List<String> classpathElements = new ArrayList<String>();
        classpathElements.add(".");

        // setup local rubygems repository
        final File rubygems = new File("target/rubygems");
        rubygems.mkdirs();

        final Logger logger = new NoopLogger();
        // no classrealm
        final ScriptFactory factory = new GemScriptFactory(logger,
                null,
                new File(""),
                classpathElements,
                false,
                rubygems,
                rubygems);

        this.gemspec = new GemspecConverter(logger, factory);

    }

    public void notestGemspec() throws Exception {
        final long start = System.currentTimeMillis();
        this.gemspec.createPom(new File("src/test/resources/test.gemspec"),
                               "0.20.0",
                               new File("target/pom.xml"));
        // assume parsing the pom is test enough here !!
        new XMLReaderAdapter().parse("target/pom.xml");
        final long end = System.currentTimeMillis();
        time = end - start;
    }

    public void notestGemspecAgain() throws Exception {
        final long oldTime = time;
        notestGemspec();
        assertTrue(time + " < " + (oldTime), time < oldTime);
    }

    public void notestUpdateMetadata() throws Exception {
        final File repo = new File(System.getProperty("user.home")
                + "/.m2/repository/rubygems");
        cleanupMetadata("rubygems-test", repo);
        final List<String> remoteRepos = new ArrayList<String>();
        remoteRepos.add("rubygems-test");
        this.gemspec.updateMetadata(remoteRepos, repo.getAbsolutePath());

        assertTrue(cleanupMetadata("rubygems-test", repo) > 1000);
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
