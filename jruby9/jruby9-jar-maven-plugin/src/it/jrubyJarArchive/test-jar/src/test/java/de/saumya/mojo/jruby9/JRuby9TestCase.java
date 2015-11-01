package de.saumya.mojo.jruby9;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.IsolatedScriptingContainer;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.*;

public class JRuby9TestCase {

    private final IsolatedScriptingContainer container = new IsolatedScriptingContainer(LocalContextScope.SINGLETHREAD);
    {
        // TODO jar-dependencies should search classloader
        container.setCurrentDirectory("uri:classloader:/");
    }
        
    
    @Test
    public void testClasspath() {
        assertEquals( "10", container.parse( "require 'jars/setup';$CLASSPATH.size").run().toString() );
    }

    @Test
    public void testScript() {
        assertEquals( "true", container.parse( "load 'test.rb';Minitest.run" ).run().toString());
    }

    @Test
    public void testWithRSpec() {
        assertEquals("0", container.parse( "require 'rspec';RSpec::Core::Runner.run([ENV_JAVA['basedir'].gsub('\\\\', '/') + '/spec/one_spec.rb'], $stderr, $stdout) " ).run().toString());
    }

}
