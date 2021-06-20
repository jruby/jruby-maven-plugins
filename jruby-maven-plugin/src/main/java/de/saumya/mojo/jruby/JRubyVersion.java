package de.saumya.mojo.jruby;

public class JRubyVersion
{
    public enum Mode {
        
        _22( "" ), _21( "" ), _20( "--2.0" ), _19( "--1.9" ), _18( "--1.8" );

        public final String flag;

        Mode(){
            this(null);
        }

        Mode( String flag ){
            this.flag = flag;
        }

        public String toString(){
            return flag == null? "" : flag.replace( "-", "" );
        }
    }
    
    private final int minor;
    private final String version;
    private final int major;
    
    public JRubyVersion( String version )
    {
        this.version = version;
        String v = version.replace( "-SNAPSHOT", "" );
        int first = v.indexOf( '.' );
        this.major = Integer.parseInt( first < 0 ? v : v.substring( 0, first ) );
        int second = v.indexOf( '.', first + 1 );
        if ( first < 0 || second < 0 )
        {
            this.minor = 0;
        }
        else
        {
            int m = 0;
            try
            {
                m = Integer.parseInt( v.substring( first + 1, second ) );
            }
            catch( NumberFormatException e )
            {
		// ignore
            }
	    this.minor = m;
        }
    }

    public Mode defaultMode()
    {
        if ( this.major == 1 ) {
            if (this.minor < 7)
            {
                return Mode._18;
            }
            else
            {
                return Mode._19;
            }
        }
        else {
            return Mode._22;
        }
    }
    
    public boolean hasMode( Mode mode )
    {
        switch( mode )
        {
        case _18:
            return this.major == 1;
        case _19:
            return this.major == 1 && this.minor > 5;
        case _20:
            return this.major == 1 && this.minor > 6;
        case _22:
            return this.major > 1;
        default:
            return false;
        }
    }

    public boolean needsOpenSSL()
    {
        return this.major == 1 && this.minor < 7;
    }
    
    public String toString()
    {
        return this.version;
    }
    
    public boolean equals( Object other )
    {
        return other != null && this.version.equals( ( (JRubyVersion) other ).version );
    }
}
