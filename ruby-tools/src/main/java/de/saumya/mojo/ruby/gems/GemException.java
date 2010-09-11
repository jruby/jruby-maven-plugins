package de.saumya.mojo.ruby.gems;

public class GemException extends Exception {
    private static final long serialVersionUID = 740727357226540997L;

    public GemException(final Exception e) {
        super(e);
    }

    public GemException(final String msg, final Exception e) {
        super(msg, e);
    }

    public GemException(final String msg) {
        super(msg);
    }

}
