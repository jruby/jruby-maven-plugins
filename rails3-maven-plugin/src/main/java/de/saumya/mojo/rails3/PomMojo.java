package de.saumya.mojo.rails3;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;

import de.saumya.mojo.ruby.script.ScriptException;

/**
 * @goal pom
 */
public class PomMojo extends AbstractRailsMojo {

    /** @parameter expression="${plugin}" @readonly */
    PluginDescriptor plugin;

    /** @parameter expression="${rails.pom.force}" default-value="false" */
    boolean          force;

    /**
     * @parameter expression="${rails.gemfile}"
     *            default-value="${basedir}/Gemfile"
     */
    File             gemfile;

    @Override
    protected void executeRails() throws MojoExecutionException, IOException,
            ScriptException {
        final File pomfile = new File(launchDirectory(), "pom.xml");

        if (this.gemfile.exists()) {
            if (!(pomfile.exists() && this.gemfile.lastModified() > pomfile.lastModified())
                    || this.force) {
                if (this.jrubyVerbose) {
                    getLog().info("create pom using following versions:");
                    getLog().info("\tjruby-plugins-version: "
                            + this.plugin.getVersion());
                    getLog().info("\tjruby-version: " + this.jrubyVersion);
                }

                // the actual execution of the script
                this.factory.newScriptFromResource("maven/tools/rails_pom_mojo.rb")
                        .addArg("{:jruby_complete => '" + this.jrubyVersion
                                + "', :jruby_plugins => '"
                                + this.plugin.getVersion() + "'}")
                        .addArg(this.gemfile.getAbsoluteFile())
                        .executeIn(launchDirectory(), pomfile);
            }
            else {
                if (this.jrubyVerbose) {
                    getLog().info("pom is newer then Gemfile. skip creation of pom. force creation with -Drails.pom.force");
                }
            }
        }
        else {
            getLog().warn("no Gemfile. nothing to do. please specify one with -Drails.gemfile=...");
        }
    }
}
