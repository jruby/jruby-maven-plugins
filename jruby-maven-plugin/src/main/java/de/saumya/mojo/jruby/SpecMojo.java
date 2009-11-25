package de.saumya.mojo.jruby;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal to run rspecs.
 * 
 * @goal spec
 * @phase integration-test
 * @requiresDependencyResolution test
 */
public class SpecMojo extends AbstractJRubyMojo {

    /**
     * directory with the spec files.
     * 
     * @parameter expression="${jruby.spec.dir}" default-value="spec"
     */
    protected File    specDirectory;

    /**
     * include pattern for files in the spec directory
     * 
     * @parameter expression="${jruby.spec.pattern}"
     *            default-value="**\/*_spec.rb"
     */
    protected String  includes;

    /**
     * file to run with spec.
     * 
     * @parameter default-value="${jruby.spec.file}"
     */
    protected File    specFile;

    /**
     * file with spec options.
     * 
     * @parameter expression="${jruby.spec.opts}" default-value="spec/spec.opts"
     * @required
     */
    protected File    specOptions;

    /**
     * skip specs.
     * 
     * @parameter expression="${skipSpecs}" default-value="false"
     *            alias="skipTests"
     * @required
     */
    protected boolean skipSpecs = false;

    /**
     * arguments for the spec command.
     * 
     * @parameter default-value="${jruby.spec.args}"
     */
    protected String  args      = null;

    @Override
    public void execute() throws MojoExecutionException {
        if (this.skipSpecs) {
            getLog().info("skip specs");
        }
        else {
            if (!this.specDirectory.exists()) {
                getLog().info(this.specDirectory
                        + " directory does not exit - skip specs");
            }
            else {
                final StringBuilder args = new StringBuilder("-S spec ");
                if (this.args != null) {
                    args.append(" ").append(this.args);
                }
                if (this.specFile != null) {
                    args.append(" ").append(this.specFile);
                }
                else {
                    args.append(" ")
                            .append(this.specDirectory.getAbsolutePath())
                            .append(" -p ")
                            .append(this.includes);
                }
                if (this.specOptions.exists()) {
                    args.append(" -O ").append(this.specOptions);
                }
                execute(args.toString());
            }
        }
    }
}
