package de.saumya.mojo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.util.FileUtils;

public class EmbeddedLauncherTest extends TestCase {

    public EmbeddedLauncherTest(final String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(EmbeddedLauncherTest.class);
    }

    private Launcher launcher;
    private final String expected = "onetwothree\n"
            + new File("./src/main").getAbsolutePath() + "\n"
            + new File("./src/test").getAbsolutePath() + "\n";
    private File home;
    private File path;

    @Override
    public void setUp() throws Exception {
        final LauncherFactory factory = new LauncherFactory();
        final List<String> classpathElements = new ArrayList<String>();
        classpathElements.add(".");
        final Map<String, String> env = new HashMap<String, String>();
        this.home = new File("./src/main");
        this.path = new File("./src/test");
        env.put("GEM_HOME", this.home.getAbsolutePath());
        env.put("GEM_PATH", this.path.getAbsolutePath());
        this.launcher = factory.getEmbeddedLauncher(true, classpathElements,
                env, new File("./pom.xml"), new ClassWorld().newRealm("test")
                        .createChildRealm("child"));

    }

    public void testExecution() throws Exception {
        this.launcher.execute("target/test-classes/test.rb", "one", "two",
                "three");
        File f = new File("target/test-classes/test.rb.txt");
        if (!f.exists()) {
            // in this case GEM_HOME was set in system environment
            f = new File("target/test-classes/test.rb-gem.txt");
        }
        assertEquals("onetwothree", FileUtils.fileRead(f)
                .replace("\n", "--n--").replaceFirst("--n--.*", ""));
    }

    public void testExecutionWithOutput() throws Exception {
        final File output = new File(
                "target/test-classes/test_with_output.rb.txt");
        this.launcher.execute(output,
                "target/test-classes/test_with_output.rb", "one", "two",
                "three");
        assertEquals("onetwothree", FileUtils.fileRead(output).replace("\n",
                "--n--").replaceFirst("--n--.*", ""));
    }

    public void testScript() throws Exception {
        this.launcher.executeScript("test.rb", "one", "two", "three");
        assertEquals(this.expected, FileUtils
                .fileRead("target/test-classes/test.rb-gem.txt"));
    }

    public void testScriptWithOutput() throws Exception {
        final File output = new File(
                "target/test-classes/test_with_output.rb-gem.txt");
        this.launcher.executeScript("test_with_output.rb", output, "one",
                "two", "three");
        assertEquals(this.expected, FileUtils.fileRead(output));
    }

    public void testGem() throws Exception {
        final File output = new File("target/test-classes/gem.txt");
        this.launcher.executeScript("META-INF/jruby.home/bin/gem", output,
                "env");
        final String[] lines = FileUtils.fileRead(output).split("\\n");
        int countDir = 0;
        int countHome = 0;
        int countPath = 0;
        for (final String line : lines) {
            if (line.contains("DIRECTORY: " + this.home.getAbsolutePath())) {
                countDir++;
            }
            if (line.contains(this.home.getAbsolutePath())) {
                countHome++;
            }
            if (line.contains(this.path.getAbsolutePath())) {
                countPath++;
            }
        }
        assertEquals(2, countDir);
        assertEquals(3, countHome);
        assertEquals(1, countPath);
    }
}
