/**
 * 
 */
package de.saumya.mojo;

import java.io.File;
import java.io.IOException;

public interface Launcher {

    public abstract void execute(File outputFile, final String... args)
            throws RubyScriptException, IOException;

    public abstract void execute(final String... args)
            throws RubyScriptException, IOException;

    public abstract void executeScript(final String scriptName,
            File outputFile, final String... args) throws RubyScriptException,
            IOException;

    public abstract void executeScript(final String scriptName,
            final String... args) throws RubyScriptException, IOException;

}