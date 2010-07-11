package de.saumya.mojo.rails3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;

import de.saumya.mojo.gem.AbstractGemMojo;

/**
 * abstract rails mojo which provides a few helper methods and the rails.args
 * parameter.
 */
public abstract class AbstractRailsMojo extends AbstractGemMojo {

    /**
     * arguments for the rails command
     * 
     * @parameter default-value="${args}"
     */
    protected String args = null;

    /**
     * @parameter expression="${rails.dir}"
     *            default-value="${project.basedir}/src/main/rails"
     */
    protected File   dir;

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
            try {
                if (IOUtil.contentEquals(new FileInputStream(boot),
                                         Thread.currentThread()
                                                 .getContextClassLoader()
                                                 .getResourceAsStream("boot.rb.orig"))) {
                    IOUtil.copy(Thread.currentThread()
                                        .getContextClassLoader()
                                        .getResourceAsStream("boot.rb"),
                                new FileOutputStream(boot));
                }
            }
            catch (final IOException e) {
                throw new MojoExecutionException("error patching config/boot.rb",
                        e);
            }
        }
    }

    protected void executeScript(final File script, final String args,
            final boolean resolveArtifacts) throws MojoExecutionException {
        patchBootScript();
        executeScript(script, args, resolveArtifacts, envParameters());
    }

    @Override
    protected void execute(final String args, final boolean resolveArtifacts)
            throws MojoExecutionException {
        patchBootScript();
        super.execute(args, resolveArtifacts, envParameters());
    }

    private Map<String, String> envParameters() {
        final File gemfile = new File(launchDirectory(), "Gemfile.maven");
        if (gemfile.exists()) {
            final Map<String, String> env = new HashMap<String, String>();
            env.put("BUNDLE_GEMFILE", gemfile.getAbsolutePath());
            return env;
        }
        else {
            // must be mutable !!!
            return new HashMap<String, String>();
        }
    }

    @Override
    protected File launchDirectory() {
        if (this.dir.exists()) {
            return this.dir;
        }
        else {
            return super.launchDirectory();
        }
    }

    protected StringBuilder binScript(final String script) {
        return new StringBuilder(new File(binDirectory(), script).getAbsolutePath());
    }

    protected File binDirectory() {
        if (this.gemHome == null) {
            if (System.getenv("GEM_HOME") == null) {
                // TODO something better is needed I guess
                return null;
            }
            else {
                return new File(System.getenv("GEM_HOME"), "bin");
            }
        }
        else {
            return new File(this.gemHome, "bin");
        }
    }

    protected File railsScriptFile() {
        return new File(new File(launchDirectory(), "script"), "rails");
    }

    protected StringBuilder railsScript(final String command) {
        final StringBuilder builder = new StringBuilder(railsScriptFile().getAbsolutePath());
        builder.append(" ").append(command);
        return builder;
    }
}
