package com.example.assembly;

import com.example.Hello;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

class Main {

    public static void main(String... args) throws Exception {
	ScriptEngineManager manager = new ScriptEngineManager();
	ScriptEngine engine = manager.getEngineByName("jruby");
	engine.eval("require 'rubygems';");
	
	System.out.println(engine.eval("begin\n" +
	    "require 'hello'\n" + 
	    "Hello.new.world\n" + 
	    "rescue LoadError\n" +
	    "'no ruby hello found'\n" + 
	    "end"));

	System.out.println(new Hello().world());
    }
}
