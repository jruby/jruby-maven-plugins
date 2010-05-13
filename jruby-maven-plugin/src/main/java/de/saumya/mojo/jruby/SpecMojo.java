package de.saumya.mojo.jruby;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal to run rspecs. (deprecated, use rspec-maven-plugin instead)
 *
 * @goal spec
 * @phase test
 * @requiresDependencyResolution test
 */
@Deprecated
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

    private String relativeFile(final File file) {
        if (this.project.getBasedir() != null) {
            return file.getAbsolutePath().substring(this.project.getBasedir()
                    .getAbsolutePath()
                    .length());
        }
        else {
            return file.getPath();
        }
    }

    private File specDirectory() {
        File specDir = this.specDirectory;
        if (!specDir.exists()) {
            specDir = new File(launchDirectory(),
                    relativeFile(this.specDirectory));
            if (specDir.exists()) {
                return specDir;
            }
        }
        return this.specDirectory;
    }

    private File specOptions() {
        File specOpts = this.specOptions;
        if (!specOpts.exists()) {
            specOpts = new File(launchDirectory(),
                    relativeFile(this.specOptions));
            if (specOpts.exists()) {
                return specOpts;
            }
        }
        return this.specOptions;
    }

    private File specFile() {
        File specFile = this.specFile;
        if (!specFile.exists()) {
            specFile = new File(launchDirectory(), relativeFile(this.specFile));
            if (specFile.exists()) {
                return specFile;
            }
        }
        return this.specFile;
    }

    public void execute() throws MojoExecutionException {
        if (this.skipSpecs) {
            getLog().info("skip specs");
        }
        else {
            final File specDir = specDirectory();
            if (!specDir.exists()) {
                getLog().info(specDir + " directory does not exit - skip specs");
            }
            else {
                ensureGem("rspec");
                final StringBuilder args = new StringBuilder("-S spec ");
                if (this.args != null) {
                    args.append(" ").append(this.args);
                }
                if (this.specFile != null) {
                    args.append(" ").append(specFile());
                }
                else {
                    args.append(" ")
                            .append(specDir.getAbsolutePath())
                            .append(" -p ")
                            .append(this.includes);
                }
                final File specOpts = specOptions();
                if (specOpts.exists()) {
                    args.append(" -O ").append(specOpts);
                }
                execute(args.toString());
            }
        }
    }
}
