package de.saumya.mojo.rails3;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.RubyScriptException;
import de.saumya.mojo.ruby.Script;

/**
 * goal to run rails rake with the given arguments.
 * 
 * @goal rake
 * @requiresDependencyResolution test
 */
public class RakeMojo extends AbstractRailsMojo {

    /**
     * arguments for the generate command
     * 
     * @parameter default-value="${rake.args}"
     */
    protected String rakeArgs = null;

    /**
     * the path to the application to be generated
     * 
     * @parameter default-value="${task}"
     */
    protected String task     = null;

    @Override
    public void executeRails() throws MojoExecutionException,
            RubyScriptException, IOException {
        final Script script = this.factory.newScriptFromResource(RAKE_RUBY_COMMAND)
                .addArgs(this.rakeArgs)
                .addArgs(this.args)
                .addArgs(this.task)
                .addArg("RAILS_ENV=" + this.env);

        final File gemfile = new File(launchDirectory(), "Gemfile.maven");
        if (gemfile.exists()) {
            script.addArg("BUNDLE_GEMFILE=" + gemfile.getAbsolutePath());
        }
        script.executeIn(launchDirectory());
    }
}
