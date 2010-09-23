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

    /** @parameter expression="${rails.pom.war}" default-value="2.1" */
    String           warVersion;

    /**
     * @parameter expression="${rails.pom.jruby-rack}"
     *            default-value="1.0.4.dev-SNAPSHOT"
     */
    String           jrubyRackVersion;

    /** @parameter expression="${rails.pom.jetty}" default-value="7.1.0.RC1" */
    String           jettyVersion;

    /** @parameter expression="${rails.pom.force}" default-value="false" */
    boolean          force;

    @Override
    protected void executeRails() throws MojoExecutionException, IOException,
            ScriptException {
        final File gemfile = new File(launchDirectory(), "Gemfile");
        final File pomfile = new File(launchDirectory(), "pom.xml");

        if (gemfile.exists()) {
            if (pomfile.exists()
                    && gemfile.lastModified() > pomfile.lastModified()
                    || this.force) {
                if (this.jrubyVerbose) {
                    getLog().info("create pom using following versions:");
                    getLog().info("\tjruby-plugins-version: "
                            + this.plugin.getVersion());
                    getLog().info("\tjetty-plugin-version: "
                            + this.jettyVersion);
                    getLog().info("\twar-plugin-version: " + this.warVersion);
                    getLog().info("\tjruby-version: " + this.jrubyVersion);
                    getLog().info("\tjruby-rack-version: "
                            + this.jrubyRackVersion);
                }

                // the actual execution of the script
                this.factory.newScriptFromResource("/maven/tools/rails_pom.rb")
                        .addArg("{:jruby_plugins => "
                                + this.plugin.getVersion()
                                + ", :jruby_complete => " + this.jrubyVersion
                                + ", :war_plugin => " + this.warVersion
                                + ", :jruby_rack => " + this.jrubyRackVersion
                                + ", :jetty_plugin => " + this.jettyVersion
                                + "}")
                        .executeIn(launchDirectory(), pomfile);

            }
            else {
                if (this.jrubyVerbose) {
                    getLog().info("pom is newer then Gemfile. skip creation of pom.");
                }
            }
        }
        else {
            getLog().warn("no Gemfile. nothing to do.");
        }
    }
}
