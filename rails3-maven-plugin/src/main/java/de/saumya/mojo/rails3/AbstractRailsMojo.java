package de.saumya.mojo.rails3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;

import de.saumya.mojo.gem.AbstractGemMojo;
import de.saumya.mojo.ruby.RubyScriptException;

/**
 * abstract rails mojo which provides a few helper methods and the rails.args
 * parameter.
 */
public abstract class AbstractRailsMojo extends AbstractGemMojo {

    /**
     * @parameter expression="${rails.dir}"
     *            default-value="${project.basedir}/src/main/rails"
     */
    protected File   railsDir;

    /**
     * either development or test or production or whatever else is possible
     * with your config
     * 
     * @parameter expression="${rails.env}"
     */
    protected String env;

    private void patchBootScript() throws MojoExecutionException {
        final File boot = new File(new File(launchDirectory(), "config"),
                "boot.rb");
        if (boot.exists()) {
            InputStream bootIn = null;
            InputStream bootOrig = null;
            InputStream bootPatched = null;
            OutputStream bootOut = null;
            try {
                bootIn = new FileInputStream(boot);
                bootOrig = Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream("boot.rb.orig");
                if (IOUtil.contentEquals(bootIn, bootOrig)) {
                    bootIn.close();
                    bootOut = new FileOutputStream(boot);
                    bootPatched = Thread.currentThread()
                            .getContextClassLoader()
                            .getResourceAsStream("boot.rb");
                    IOUtil.copy(bootPatched, bootOut);
                }
            }
            catch (final IOException e) {
                throw new MojoExecutionException("error patching config/boot.rb",
                        e);
            }
            finally {
                IOUtil.close(bootIn);
                IOUtil.close(bootOrig);
                IOUtil.close(bootPatched);
                IOUtil.close(bootOut);
            }
        }
    }

    private void setupEnvironmentVariables() {
        final File gemfile = new File(launchDirectory(), "Gemfile.maven");
        if (gemfile.exists()) {
            this.factory.addEnv("BUNDLE_GEMFILE", gemfile);
        }
    }

    @Override
    public final void executeWithGems() throws MojoExecutionException,
            RubyScriptException, IOException {

        setupEnvironmentVariables();

        patchBootScript();

        executeRails();
    }

    abstract void executeRails() throws MojoExecutionException,
            RubyScriptException, IOException;

    @Override
    protected File launchDirectory() {
        if (this.railsDir.exists()) {
            return this.railsDir;
        }
        else {
            return super.launchDirectory();
        }
    }

    protected File railsScriptFile() {
        return new File(new File(launchDirectory(), "script"), "rails");
    }
}
