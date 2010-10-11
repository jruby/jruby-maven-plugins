package de.saumya.mojo.ruby.rails;

public class RailsException
    extends Exception
{

    private static final long serialVersionUID = 5463785987091179445L;

    public RailsException(final Exception e)
    {
        super( e );
    }

    public RailsException(final String msg, final Exception e)
    {
        super( msg, e );
    }

    public RailsException(final String msg)
    {
        super( msg );
    }

}
