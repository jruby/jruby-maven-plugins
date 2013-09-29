package de.saumya.mojo.tests;

import java.util.ArrayList;
import java.util.List;

import de.saumya.mojo.jruby.JRubyVersion;

public class JRubyRun {

    public enum Mode {
        _20("--2.0"), _19("--1.9"), _18("--1.8"), _18_19, _18_19_20, DEFAULT;

        public final String flag;

        Mode(){
            this(null);
        }

        Mode(String flag){
            this.flag = flag;
        }

        public String toString(){
            return flag == null? "" : flag.replace("-", "");
        }
    }

    public static class Result {
        public boolean success;
        public String message;
    }

    public final Mode mode;

    public final JRubyVersion version;

    final Result[] results = new Result[Mode.values().length];

    public JRubyRun(Mode mode, String version){
        this.mode = mode;
        this.version = new JRubyVersion( version );
    }

    public JRubyRun.Mode[] asSingleModes(){
        switch (mode){
        case _18_19_20:
            List<JRubyRun.Mode> modes = new ArrayList<JRubyRun.Mode>();            
            if ( version.hasMode18() ) {
                modes.add(  Mode._18 );
            }
            if ( version.hasMode19() ) {
                modes.add(  Mode._19 );
            }
            if ( version.hasMode20() ) {
                modes.add(  Mode._20 );
            }
            return modes.toArray( new JRubyRun.Mode[ modes.size() ] );
        case _18_19:
            modes = new ArrayList<JRubyRun.Mode>();            
            if ( version.hasMode18() ) {
                modes.add(  Mode._18 );
            }
            if ( version.hasMode19() ) {
                modes.add(  Mode._19 );
            }
            return modes.toArray( new JRubyRun.Mode[ modes.size() ] );
        case _19:
            if(version.hasMode19()) {
                return new JRubyRun.Mode[] {Mode._19};
            }
            else {
                return new JRubyRun.Mode[0];
            }
        case _18:
            if(version.hasMode18()) {
                return new JRubyRun.Mode[] {Mode._18};
            }
            else {
                return new JRubyRun.Mode[0];
            }
        default:
            return new JRubyRun.Mode[] {mode};
        }
    }

    public Result result(Mode mode){
        return results[mode.ordinal()];
    }

    public void setResult(Mode mode, Result result){
        results[mode.ordinal()] = result;
    }

    public String toString(Mode mode){
        Result result = result(mode);
        return "jruby-" + version + " mode " + mode + ": " + result.message;
    }
}
