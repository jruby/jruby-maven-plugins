package de.saumya.mojo.gem;

public class JRubyVersion
{
    private int minor;

    JRubyVersion( String version )
    {
        int first = version.indexOf( '.' );
        //this.major = Integer.parseInt( version.substring( 0, first ) );
        this.minor = Integer.parseInt( version.substring( first + 1, version.indexOf( '.', first + 1 ) ) );
    }

    boolean needsOpenSSL()
    {
        return this.minor < 7;
    }
}
