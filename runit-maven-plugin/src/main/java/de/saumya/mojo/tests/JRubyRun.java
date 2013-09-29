package de.saumya.mojo.tests;

import java.util.ArrayList;
import java.util.List;

import de.saumya.mojo.jruby.JRubyVersion;
import de.saumya.mojo.jruby.JRubyVersion.Mode;

public class JRubyRun {

    public static class Result {
        public boolean success;
        public String message;
    }

    public final Mode[] modes;

    public final JRubyVersion version;

    final Result[] results = new Result[Mode.values().length];

    private static Mode[] toModes( String modes )
    {
        String[] m = modes.split( "[\\ ,;]+" );
        Mode[] result = new Mode[ m.length ];
        int i = 0;
        for( String mode : m )
        {
            result[ i++ ] = Mode.valueOf( "--" + mode );
        }
        return result;
    }

    public JRubyRun( JRubyVersion version ){
        this( version, version.defaultMode() );
    }
     
    public JRubyRun( String version, String modes ){
        this( version, toModes( modes ) );
    }

    public JRubyRun( String version, Mode... modes ){
        this( new JRubyVersion( version ), modes );
    }

    public JRubyRun( JRubyVersion version, Mode... modes ){
        this.modes = modes.length == 0 ? new Mode[] { version.defaultMode() }: modes;
        this.version = version;
    }
    
    public boolean isDefaultModeOnly()
    {
        return modes.length == 1 && version.defaultMode() == modes[ 0 ];  
    }

    public Result result(Mode mode){
        return results[ mode.ordinal() ];
    }

    public void setResult(Mode mode, Result result){
        results[mode == null ? version.defaultMode().ordinal() : mode.ordinal()] = result;
    }

    public String toString(Mode mode){
        Result result = result(mode);
        return "jruby-" + version + " mode " + mode + ": " + result.message;
    }
}
