package de.saumya.mojo.rails;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.saumya.mojo.ruby.script.ScriptException;

/**
 * Goal to run rails generator script.
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
    public void execute() throws MojoExecutionException, MojoFailureException {
        // make sure the whole things run in the same process
        this.jrubyFork = false;
        super.execute();
    }

    @Override
    protected void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException {
        this.factory.newScript(railsScriptFile("generate"))
                .addArg(this.generator)
                .addArgs(this.generateArgs)
                .addArgs(this.args)
                .executeIn(launchDirectory());
    }
}
