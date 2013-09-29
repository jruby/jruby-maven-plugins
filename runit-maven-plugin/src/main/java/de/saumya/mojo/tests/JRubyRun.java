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

    public final boolean isDefaultModeOnly;

    final Result[] results = new Result[Mode.values().length];
    
    private static Mode[] filter( JRubyVersion version, Mode[] modes )
    {
        List<Mode> result = new ArrayList<Mode>();
        for( Mode m: modes )
        {
            if ( version.hasMode( m ) )
            {
                result.add( m );
            }
        }
        return result.toArray( new Mode[ result.size() ] );
    }

    public JRubyRun( JRubyVersion version ){
        this( true, version, version.defaultMode() );
    }

    public JRubyRun( JRubyVersion version, Mode... modes ){
        this( false, version, modes );
    }
    public JRubyRun( boolean isDefault, JRubyVersion version, Mode... modes ){
        this.modes = modes.length == 0 ? new Mode[] { version.defaultMode() }: filter( version, modes );
        this.version = version;
        this.isDefaultModeOnly = isDefault;
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
