package de.saumya.mojo.ruby;

import java.io.File;

import de.saumya.mojo.ruby.script.ScriptException;

public interface GemService {

    public File binDirectory() throws ScriptException;

    public File gemDirectory() throws ScriptException;

    public File binScriptFile(final String script) throws ScriptException;

    // public String binScript(final String script) throws RubyScriptException;

}
