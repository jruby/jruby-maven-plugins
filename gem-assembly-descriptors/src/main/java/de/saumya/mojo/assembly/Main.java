package de.saumya.mojo.assembly;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

class Main {

    public static void main(String... args) throws Exception {
        if (args.length == 0){
            System.err.println("missing name of bin-script as argument");
            System.exit(-1);
        }
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("jruby");
        engine.eval("require 'rubygems'");
        engine.eval("ARGV.delete_at(0)");
        engine.eval("load '" + args[0] + "'");
    }
}
