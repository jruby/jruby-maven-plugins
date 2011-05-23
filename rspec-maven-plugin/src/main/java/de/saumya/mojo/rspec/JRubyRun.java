/**
 * 
 */
package de.saumya.mojo.rspec;

public class JRubyRun {

    enum Mode {
        _19("--1.9"), _18("--1.8"), _18_19, DEFAULT;
        
        final String flag;
        
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
    
    final JRubyRun.Mode mode;
    
    final String version;
    
    final boolean[] success = new boolean[4];
    
    final String[] message = new String[4];
    
    JRubyRun(JRubyRun.Mode mode, String version){
        this.mode = mode;
        this.version = version;
    }
    
    boolean doesVersionAllow19(){
        // only for jruby version bigger then 1.6.0 has 1.9 support
        return version == null ? false : version.charAt(2) >= '6';
    }
    
    JRubyRun.Mode[] asSingleModes(){
        switch (mode){
        case _18_19:
            if(doesVersionAllow19()) {
                return new JRubyRun.Mode[] {Mode._18, Mode._19};
            }
            else {
                return new JRubyRun.Mode[] {Mode._18};
            }
        case _19:
            if(doesVersionAllow19()) {
                return new JRubyRun.Mode[] {Mode._19};
            }
            else {
                return new JRubyRun.Mode[0];
            }
        default:
            return new JRubyRun.Mode[] {mode};
        }
    }
            
    boolean success(JRubyRun.Mode mode){
        return success[mode.ordinal()];
    }
    
    void success(JRubyRun.Mode mode, boolean success){
        this.success[mode.ordinal()] = success;
    }
    
    String message(JRubyRun.Mode mode){
        return message[mode.ordinal()];
    }

    void message(JRubyRun.Mode mode, String msg){
        message[mode.ordinal()] = msg;
    }
    
    String toString(JRubyRun.Mode mode){
        return "jruby-" + version + " mode " + mode + (success(mode) ? " passed: " : " failed: " ) + message(mode);
    }
}