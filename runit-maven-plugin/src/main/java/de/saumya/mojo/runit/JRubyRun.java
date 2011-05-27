/**
 *
 */
package de.saumya.mojo.runit;

public class JRubyRun {

    public enum Mode {
        _19("--1.9"), _18("--1.8"), _18_19, DEFAULT;

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

    public final String version;

    final Result[] results = new Result[4];

    public JRubyRun(JRubyRun.Mode mode, String version){
        this.mode = mode;
        this.version = version;
    }

    boolean doesVersionAllow19(){
        // only for jruby version bigger then 1.6.0 has 1.9 support
        return version == null ? false : version.charAt(2) >= '6';
    }

    public JRubyRun.Mode[] asSingleModes(){
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