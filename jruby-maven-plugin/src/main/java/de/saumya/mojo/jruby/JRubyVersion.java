package de.saumya.mojo.jruby;

public class JRubyVersion
{
    public enum Mode {
        
        _20( "--2.0" ), _19( "--1.9" ), _18( "--1.8" );

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
    
    private int minor;
    private final String version;
    
    public JRubyVersion( String version )
    {
        this.version = version;
        int first = this.version.indexOf( '.' );
        //this.major = Integer.parseInt( version.substring( 0, first ) );
        this.minor = Integer.parseInt( this.version.substring( first + 1, this.version.indexOf( '.', first + 1 ) ) );
    }

    public Mode defaultMode()
    {
        if (this.minor < 7)
        {
            return Mode._18;
        }
        else
        {
            return Mode._19;
        }
    }
    
    public boolean hasMode( Mode mode )
    {
        switch( mode )
        {
        case _18:
            return true;
        case _19:
            return this.minor > 5;
        case _20:
            return this.minor > 6;
         default:
             throw new RuntimeException( "BUG" );
        }
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
    
    public boolean equals( Object other )
    {
        return other != null && this.version.equals( ( (JRubyVersion) other ).version );
    }
}
