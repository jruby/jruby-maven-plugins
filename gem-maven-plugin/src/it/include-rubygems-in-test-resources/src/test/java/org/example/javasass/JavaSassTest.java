package org.example.javasass;

import static org.junit.Assert.assertEquals;

import org.jruby.embed.ScriptingContainer;

import org.junit.Test;

public class JavaSassTest {

    @Test
    public void testJavaSass() throws Exception {
        ScriptingContainer container = new ScriptingContainer();
        container.runScriptlet("require 'rubygems'; require 'sass';");

        String sass = ".test\n\tcolor: red";
        container.put("str", sass);
        
        String css = (String)container.runScriptlet("Sass::Engine.new(str).render");

        assertEquals(".test {\n  color: red; }\n", css);
    }

}
