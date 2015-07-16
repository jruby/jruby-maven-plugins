package de.saumya.mojo.jruby9;

import de.saumya.mojo.gem.AbstractGemMojo;

public abstract class AbstractJRuby9Mojo extends AbstractGemMojo {

    @Override
    protected String getDefaultJRubyVersion() {
        return Versions.JRUBY;
    }
    
}