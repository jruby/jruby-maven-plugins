package de.saumya.mojo.rails3;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.ruby.script.ScriptException;

/**
 * @goal pom
 */
public class PomMojo extends AbstractRailsMojo {

    /**
     * @parameter expression="${pom}" default-value="pom.xml"
     */
    File    pom;

    /** @parameter expression="${pom.force}" default-value="false" */
    boolean          force;

    /**
     * @parameter expression="${pom.gemfile}"
     *            default-value="${basedir}/Gemfile"
     */
    File             gemfile;

    @Override
    protected void executeRails() throws MojoExecutionException, IOException,
            ScriptException {
        if (this.pom.exists() && !this.force) {
            getLog().info(this.pom.getName()
                    + " already exists. use '-Dpom.force=true' to overwrite");
            return;
        }

        if (this.gemfile.exists()) {
            if (!(this.pom.exists() && this.gemfile.lastModified() > this.pom.lastModified())
                    || this.force) {
                long stamp = -1;
                if (this.pom.exists()) {
                    stamp = this.pom.lastModified();
                }
                if (this.jrubyVerbose) {
                    getLog().info("create pom using following versions:");
                    getLog().info("\tjruby-plugins-version: "
                            + this.plugin.getVersion());
                    getLog().info("\tjruby-version: " + getJrubyVersion());
                }

                this.factory.newScriptFromResource("maven/tools/pom_generator.rb")
                    .addArg("rails")
                    .addArg(this.gemfile.getAbsoluteFile())
                    .addArg(this.plugin.getVersion())
                    .addArg(getJrubyVersion().toString())
                    .executeIn(launchDirectory(), this.pom);
                
                if (stamp > -1) {
                    this.pom.setLastModified(stamp);
                }
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
