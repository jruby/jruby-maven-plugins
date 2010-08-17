/**
 * 
 */
package de.saumya.mojo.ruby;

import java.io.File;
import java.io.IOException;


public interface Launcher {

    public abstract void execute(final File outputFile, final String... args)
            throws RubyScriptException, IOException;

    public abstract void execute(final String... args)
            throws RubyScriptException, IOException;

    public abstract void executeIn(final File launchDirectory,
            final File outputFile, final String... args)
            throws RubyScriptException, IOException;

    public abstract void executeIn(final File launchDirectory,
            final String... args) throws RubyScriptException, IOException;

    public abstract void executeScript(final String scriptName,
            final File outputFile, final String... args)
            throws RubyScriptException, IOException;

    public abstract void executeScript(final File launchDirectory,
            final String scriptName, final File outputFile,
            final String... args) throws RubyScriptException, IOException;

    public abstract void executeScript(final String scriptName,
            final String... args) throws RubyScriptException, IOException;

    public abstract void executeScript(final File launchDirectory,
            final String scriptName, final String... args)
            throws RubyScriptException, IOException;

}