package de.saumya.mojo.ruby.script;

public class ScriptException extends Exception {
    private static final long serialVersionUID = 740727357226540997L;

    public ScriptException(final Exception e) {
        super(e);
    }

    public ScriptException(final String msg, final Exception e) {
        super(msg, e);
    }

    public ScriptException(final String msg) {
        super(msg);
    }

}
