package com.example.javasass;


import org.jruby.embed.ScriptingContainer;

public class JavaSassMain {

    public static void main(String[] args) {
        ScriptingContainer container = new ScriptingContainer();
        container.runScriptlet("require 'rubygems'; require 'sass';");

        String sass = ".test\n\tcolor: red";
        container.put("str", sass);

        String css = (String)container.runScriptlet("Sass::Engine.new(str).render");

        System.out.println(css);
    }
}
