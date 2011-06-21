package com.example.assembly;

import com.example.Hello;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

class Main {

    public static void main(String... args) throws Exception {
	ScriptEngineManager manager = new ScriptEngineManager();
	ScriptEngine engine = manager.getEngineByName("jruby");
	engine.eval("require 'rubygems'; require 'hello';");
	
	System.out.println(engine.eval("Hello.new.world"));

	System.out.println(new Hello().world());
    }
}
