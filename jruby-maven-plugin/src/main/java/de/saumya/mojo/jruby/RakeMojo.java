package de.saumya.mojo.jruby;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * maven wrapper around the rake command.
 * 
 * @goal rake
 * @requiresDependencyResolution test
 */
public class RakeMojo extends AbstractJRubyMojo {
    /**
     * rakefile to be used for the rake command.
     * 
     * @parameter default-value="${jruby.rakefile}"
     */
    private final File   rakefile        = null;

    /**
     * output directory for internal use.
     * 
     * @parameter expression="${project.build.directory}"
     * @required
     * @readOnly
     */
    private final File   outputDirectory = null;

    /**
     * ruby script which rakes executes as rakefile.
     * 
     * @parameter default-value="${jruby.rake.script}"
     */
    private final String script          = null;

    /**
     * arguments for the rake command.
     * 
     * @parameter default-value="${jruby.rake.args}"
     */
    private final String args            = null;

    @Override
    public void execute() throws MojoExecutionException {
        this.outputDirectory.mkdirs();
        ensureGem("rake");

        final StringBuilder args = new StringBuilder("-S rake");
        if (this.script != null) {
            final File scriptFile = new File(this.outputDirectory,
                    "rake_script.rb");
            try {
                final FileWriter writer = new FileWriter(scriptFile);
                writer.write(this.script);
                writer.close();
            }
            catch (final IOException io) {
                throw new MojoExecutionException("error writing temporary script");
            }
            args.append("-f").append(scriptFile.getAbsolutePath());
        }
        else if (this.rakefile != null) {
            args.append("-f").append(this.rakefile);
        }
        if (this.args != null) {
            args.append(" ").append(this.args);
        }
        execute(args.toString());
    }
}