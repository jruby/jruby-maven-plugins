/**
 * 
 */
package de.saumya.mojo.ruby.script;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

public class Script extends Arguments {

    private final ScriptFactory scriptFactory;
    private final String        script;

    Script(final ScriptFactory scriptFactory) {
        this.scriptFactory = scriptFactory;
        this.script = null;
    }

    Script(final ScriptFactory scriptFactory, final String script) {
        this.scriptFactory = scriptFactory;
        this.script = script;
    }

    Script(final ScriptFactory scriptFactory, final URL url) {
        this(scriptFactory, url.toString(), false);
    }

    Script(final ScriptFactory scriptFactory, final File file) {
        this(scriptFactory, file.getAbsolutePath(), false);
    }

    Script(final ScriptFactory scriptFactory, final String file,
            final boolean search) {
        this.scriptFactory = scriptFactory;
        if (search) {
            add("-S");
            add(file);
            this.script = null;
        }
        else {
            this.script = "load('" + file + "')";
        }
    }

    public boolean isValid() {
        return this.list.size() > 0 || this.script != null;
    }

    public Script addArg(final File name) {
        super.add(name.getAbsolutePath());
        return this;
    }

    public Script addArg(final String name) {
        super.add(name);
        return this;
    }

    public Script addArg(final String name, final String value) {
        if (value != null) {
            super.add(name, value);
        }
        return this;
    }

    public Script addArg(final String name, final File value) {
        if (value != null) {
            super.add(name, value.getAbsolutePath());
        }
        return this;
    }

    public Script addArgs(final String line) {
        super.parseAndAdd(line);
        return this;
    }

    public void execute() throws ScriptException, IOException {
        if (this.script != null) {
            this.scriptFactory.launcher.executeScript(this.script, this.list);
        }
        else {
            this.scriptFactory.launcher.execute(this.list);
        }
    }

    public void execute(final File output) throws ScriptException,
            IOException {
        if (this.script != null) {
            this.scriptFactory.launcher.executeScript(this.script,
                                                      this.list,
                                                      output);
        }
        else {
            this.scriptFactory.launcher.execute(this.list, output);
        }
    }

    public void execute(final OutputStream output) throws ScriptException,
            IOException {
        if (this.script != null) {
            this.scriptFactory.launcher.executeScript(this.script,
                    this.list,
                    output);
        }
        else {
            this.scriptFactory.launcher.execute(this.list, output);
        }
    }

    public void executeIn(final File launchDirectory)
            throws ScriptException, IOException {
        if (this.script != null) {
            this.scriptFactory.launcher.executeScript(launchDirectory,
                                                      this.script,
                                                      this.list);
        }
        else {
            this.scriptFactory.launcher.executeIn(launchDirectory, this.list);
        }
    }

    public void executeIn(final File launchDirectory, final File output)
            throws ScriptException, IOException {
        if (this.script != null) {
            this.scriptFactory.launcher.executeScript(launchDirectory,
                                                      this.script,
                                                      this.list,
                                                      output);
        }
        else {
            this.scriptFactory.launcher.executeIn(launchDirectory,
                                                  this.list,
                                                  output);
        }
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        if (this.script != null) {
            buf.append(this.script).append(" ");
        }
        for (final String arg : this.list) {
            buf.append(arg).append(" ");
        }
        return buf.toString().trim();
    }
}