/**
 *
 */
package de.saumya.mojo.ruby.script;

import de.saumya.mojo.ruby.Logger;
import de.saumya.mojo.ruby.ScriptUtils;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.DuplicateRealmException;
import org.codehaus.classworlds.NoSuchRealmException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptFactory {

    public static List<String> NO_CLASSPATH = Collections.emptyList();

    final Arguments switches = new Arguments();
    final Arguments jvmArgs = new Arguments();

    private final Map<String, String> env = new HashMap<String, String>();
    final File jrubyStdlibJar;

    final Logger logger;
    final ClassRealm classRealm;
    final File jrubyJar;
    final List<String> classpathElements;
    final boolean fork;

    final Launcher launcher;

    public ScriptFactory(final Logger logger, final ClassRealm classRealm,
                         final File jrubyJar,
                         final List<String> classpathElements, final boolean fork) throws ScriptException, IOException {
        this(logger, classRealm, jrubyJar, jrubyJar, classpathElements, fork);
    }

    public ScriptFactory(final Logger logger, final ClassRealm classRealm,
                         final File jrubyJar, File stdlibJar,
                         final List<String> classpathElements, final boolean fork) throws ScriptException, IOException {
        this.logger = logger;
        this.jrubyStdlibJar = stdlibJar;
        this.jrubyJar = jrubyJar;
        if (this.jrubyJar != null) {
            this.logger.debug("script uses jruby jar:" + this.jrubyJar.getAbsolutePath());
        }

        this.classpathElements = classpathElements == null
                ? NO_CLASSPATH
                : Collections.unmodifiableList(classpathElements);
        this.fork = fork;
        if (classRealm != null && jrubyJar != null) {
            this.classRealm = getOrCreateClassRealm(classRealm, jrubyJar);
        } else {
            this.classRealm = classRealm;
        }

        if (fork) {
            this.launcher = new AntLauncher(logger, this);
        } else {

            this.launcher = new EmbeddedLauncher(logger, this);
        }
    }

    private static synchronized ClassRealm getOrCreateClassRealm(final ClassRealm classRealm, final File jrubyJar) throws MalformedURLException, ScriptException {
        ClassRealm jruby;
        try {
            jruby = classRealm.getWorld().getRealm("jruby");
        } catch (final NoSuchRealmException e) {
            try {
                jruby = classRealm.getWorld().newRealm("jruby");
                if (jrubyJar != null) {
                    jruby.addConstituent(jrubyJar.toURI().toURL());
                }
            } catch (final DuplicateRealmException ee) {
                throw new ScriptException("could not setup classrealm for jruby",
                        ee);
            }
        }
        return jruby;
    }

    public Script newScriptFromSearchPath(final String scriptName)
            throws IOException {
        return new Script(this, scriptName, true);
    }

    public Script newScriptFromJRubyJar(final String scriptName)
            throws IOException {
        try {
            // the first part only works on jruby-complete.jar
            URL url = new URL("jar:file:"
                    + this.jrubyStdlibJar.getAbsolutePath()
                    + "!/META-INF/jruby.home/bin/" + scriptName);
            url.openConnection().getContent();
            return new Script(this, url);
        } catch (Exception e) {
            try {
                // fallback on classloader
                return newScriptFromResource("META-INF/jruby.home/bin/" + scriptName);
            } catch (FileNotFoundException ee) {
                // find jruby-home
                String base = ".";
                if (this.env.containsKey("JRUBY_HOME")) {
                    base = this.env.get("JRUBY_HOME");
                } else if (System.getProperty("jruby.home") != null) {
                    base = System.getProperty("jruby.home");
                } else {
                    for (String arg : this.jvmArgs.list) {
                        if (arg.startsWith("-Djruby.home=")) {
                            base = arg.substring("-Djruby.home=".length());
                            break;
                        }
                    }
                }
                File f = new File(base, new File("bin", scriptName).getPath());
                if (f.exists()) {
                    return new Script(this, f);
                } else {
                    throw ee;
                }
            }
        }
    }

    public Script newScriptFromResource(final String scriptName)
            throws IOException {
        URL url = this.classRealm != null ? this.classRealm.getClassLoader()
                .getResource(scriptName) : null;
        if (url == null) {
            url = ScriptUtils.getScriptFromResource(scriptName);
        }
        if (url.getProtocol().equals("file")) {
            return new Script(this, url.getPath(), false);
        } else {
            return new Script(this, url);
        }
    }

    public Script newArguments() {
        return new Script(this);
    }

    public Script newScript(final String script) throws IOException {
        return new Script(this, script);
    }

    public Script newScript(final File file) {
        return new Script(this, file);
    }

    public void addJvmArgs(final String args) {
        this.jvmArgs.parseAndAdd(args);
    }

    public void addSwitch(final String name) {
        this.switches.add(name);
    }

    public void addSwitch(final String name, final String value) {
        this.switches.add(name, value);
    }

    public void addSwitches(final String switches) {
        this.switches.parseAndAdd(switches);
    }

    public void addEnv(final String name, final File value) {
        if (value != null) {
            this.env.put(name, value.getAbsolutePath());
        } else {
            this.env.put(name, null);
        }
    }

    public Map<String, String> environment() {
        return env;
    }

    public void addEnv(final String name, final String value) {
        this.env.put(name, value);
    }

    public void addEnvs(final String environmentVars) {
        for (final String var : environmentVars.split("\\s+")) {
            final int index = var.indexOf("=");
            if (index > -1) {
                this.env.put(var.substring(0, index), var.substring(index + 1));
            }
        }
    }

    /**
     * Returns JRuby version representation found.
     * {@code null} if version format could not be processed.
     */
    public JRubyVersion getVersion() {
        try {
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            this.newArguments().addArg("-v").execute(os);
            final String[] versionParts = os.toString(StandardCharsets.UTF_8.toString()).split(" ");
            final String jrubyVersion = versionParts[1];
            final String languageVersion = extractLanguageVersion(versionParts[2]);
            return new JRubyVersion(jrubyVersion, languageVersion);
        } catch (Exception e) {
            logger.warn("Could not identify version: " + e.getMessage());
            return null;
        }
    }

    private String extractLanguageVersion(String versionPart) {
        return versionPart
                .replaceAll("\\(|\\)|ruby-", "");
    }

    @Override
    public String toString() {
        // TODO
        final StringBuilder buf = new StringBuilder(getClass().getName());
        // for (final String arg : this.switches) {
        // buf.append(arg).append(" ");
        // }
        return buf.toString().trim();
    }

}