package de.saumya.mojo.jruby;

public class JRubyVersion
{
    private int minor;
    private final String version;
    
    JRubyVersion( String version )
    {
        this.version = version;
        int first = this.version.indexOf( '.' );
        //this.major = Integer.parseInt( version.substring( 0, first ) );
        this.minor = Integer.parseInt( this.version.substring( first + 1, this.version.indexOf( '.', first + 1 ) ) );
    }

    public boolean needsOpenSSL()
    {
        return this.minor < 7;
    }
    
    public boolean hasJRubycVerbose()
    {
        return this.minor > 5;
    }
    
    public String toString()
    {
        return this.version;
    }
}
