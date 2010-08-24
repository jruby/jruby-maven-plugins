package de.saumya.mojo.ruby;

import java.io.File;

public interface GemService {

    public File binDirectory() throws RubyScriptException;

    public File gemDirectory() throws RubyScriptException;

    public File binScriptFile(final String script) throws RubyScriptException;

    // public String binScript(final String script) throws RubyScriptException;

}
