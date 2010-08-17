package de.saumya.mojo.ruby;

public class RubyScriptException extends Exception {
    private static final long serialVersionUID = 740727357226540997L;

    public RubyScriptException(final Exception e) {
        super(e);
    }

    public RubyScriptException(final String msg, final Exception e) {
        super(msg, e);
    }

    public RubyScriptException(final String msg) {
        super(msg);
    }

}
