package de.saumya.mojo.ruby;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class AbstractLauncher implements Launcher {

    protected abstract void doExecute(final File launchDirectory,
            final List<String> args, final File outputFile)
            throws RubyScriptException, IOException;

    public void execute(final List<String> args) throws RubyScriptException,
            IOException {
        doExecute(null, args, null);
    }

    public void execute(final List<String> args, final File outputFile)
            throws RubyScriptException, IOException {
        doExecute(null, args, outputFile);
    }

    public void executeIn(final File launchDirectory, final List<String> args)
            throws RubyScriptException, IOException {
        doExecute(launchDirectory, args, null);
    }

    public void executeIn(final File launchDirectory, final List<String> args,
            final File outputFile) throws RubyScriptException, IOException {
        doExecute(launchDirectory, args, outputFile);
    }

    public void executeScript(final String script, final List<String> args)
            throws RubyScriptException, IOException {
        executeScript(script, args, null);
    }

    public void executeScript(final String script, final List<String> args,
            final File outputFile) throws RubyScriptException, IOException {
        executeScript(null, script, args, outputFile);
    }

    public void executeScript(final File launchDirectory, final String script,
            final List<String> args) throws RubyScriptException, IOException {
        executeScript(launchDirectory, script, args, null);
    }
}
