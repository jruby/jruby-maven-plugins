package org.example.javasass;

import static org.junit.Assert.assertEquals;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

import org.junit.Test;

public class JavaSassTest {

	@Test
	public void testJavaSass() throws Exception {

		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("jruby");
		engine.eval("ENV['GEM_HOME'] = 'unknown'");
		engine.eval("ENV['GEM_PATH'] = 'unknown'");
		engine.eval("require 'rubygems'; require 'sass';");

		String sass = ".test\n\tcolor: red";

		SimpleBindings bindings = new SimpleBindings();
		bindings.put("str", sass);
		String css = (String) engine.eval("Sass::Engine.new($str).render", bindings);

		assertEquals(".test {\n  color: red; }\n", css);
	}

}
