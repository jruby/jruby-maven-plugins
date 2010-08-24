package de.saumya.mojo.rails3;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.RubyScriptException;

/**
 * goal to run a generator
 * 
 * @goal generate
 * @requiresDependencyResolution test
 */
public class GenerateMojo extends AbstractRailsMojo {

    /**
     * arguments for the generate command
     * 
     * @parameter default-value="${generate.args}"
     */
    protected String generateArgs = null;

    /**
     * the name of the generator
     * 
     * @parameter default-value="${generator}"
     */
    protected String generator    = null;

    @Override
    protected void executeRails() throws MojoExecutionException,
            RubyScriptException, IOException {
        this.factory.newScript(railsScriptFile())
                .addArg("generate")
                .addArg(this.generator)
                .addArgs(this.generateArgs)
                .addArgs(this.args)
                .executeIn(launchDirectory());
    }
}
