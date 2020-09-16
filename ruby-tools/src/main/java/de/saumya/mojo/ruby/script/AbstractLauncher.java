package de.saumya.mojo.ruby.script;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

abstract class AbstractLauncher implements Launcher {

    protected abstract void doExecute(final File launchDirectory,
                                      final List<String> args, final File outputFile)
            throws ScriptException, IOException;

    protected abstract void doExecute(final File launchDirectory,
                                      final List<String> args, final OutputStream outputStream)
            throws ScriptException, IOException;

    public void execute(final List<String> args) throws ScriptException,
            IOException {
        doExecute(null, args, (File) null);
    }

    public void execute(final List<String> args, final File outputFile)
            throws ScriptException, IOException {
        doExecute(null, args, outputFile);
    }

    public void execute(final List<String> args, final OutputStream outputStream)
            throws ScriptException, IOException {
        doExecute(null, args, outputStream);
    }

    public void executeIn(final File launchDirectory, final List<String> args)
            throws ScriptException, IOException {
        doExecute(launchDirectory, args, (File) null);
    }

    public void executeIn(final File launchDirectory, final List<String> args,
                          final File outputFile) throws ScriptException, IOException {
        doExecute(launchDirectory, args, outputFile);
    }

    public void executeIn(final File launchDirectory, final List<String> args,
                          final OutputStream outputStream) throws ScriptException, IOException {
        doExecute(launchDirectory, args, outputStream);
    }

    public void executeScript(final String script, final List<String> args)
            throws ScriptException, IOException {
        executeScript(script, args, (File) null);
    }

    public void executeScript(final String script, final List<String> args,
                              final File outputFile) throws ScriptException, IOException {
        executeScript(null, script, args, outputFile);
    }

    public void executeScript(final String script, final List<String> args,
                              final OutputStream outputStream) throws ScriptException, IOException {
        executeScript(null, script, args, outputStream);
    }

    public void executeScript(final File launchDirectory, final String script,
                              final List<String> args) throws ScriptException, IOException {
        executeScript(launchDirectory, script, args, (File) null);
    }

}
