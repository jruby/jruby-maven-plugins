package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;

import de.saumya.mojo.jruby.AbstractJRubyMojo;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * goal to converts a gemspec file into pom.xml.
 * 
 * @goal pom
 */
public class PomMojo extends AbstractJRubyMojo {

    /** @parameter expression="${plugin}" @readonly */
    PluginDescriptor  plugin;

    /**
     * the pom file to generate
     * <br/>
     * Command line -Dpom=...
     * 
     * @parameter expression="${pom}" default-value="pom.xml"
     */
    protected File    pom;

    /**
     * force overwrite of an existing pom
     * <br/>
     * Command line -Dpom.force=...
     * 
     * @parameter default-value="${pom.force}"
     */
    protected boolean force = false;

    /**
     * use a gemspec file to generate a pom
     * <br/>
     * Command line -Dpom.gemspec=...
     * 
     * @parameter default-value="${pom.gemspec}"
     */
    protected File    gemspec;

    /**
     * use Gemfile to generate a pom
     * <br/>
     * Command line -Dpom.gemfile=...
     * 
     * @parameter expression="${pom.gemfile}"
     *            default-value="Gemfile"
     */
    protected File    gemfile;

    @Override
    public void executeJRuby() throws MojoExecutionException, ScriptException, IOException {
        if (this.pom.exists() && !this.force) {
            getLog().info(this.pom.getName()
                    + " already exists. use '-Dpom.force=true' to overwrite");
            return;
        }
        if (!this.gemfile.exists()){
            this.gemfile = null;
            if (this.gemspec == null) {
                getLog().debug("no gemspec file given, see if there is single one");
                for (final File file : (this.project.getBasedir() == null
                        ? new File(".")
                        : this.project.getBasedir()).listFiles()) {
                    if (file.getName().endsWith(".gemspec")) {
                        if (this.gemspec != null) {
                            getLog().info("there is no gemspec file given but there are more then one in the current directory.");
                            getLog().info("use '-Dpom.gemspec=...' to select the gemspec file or -Dpom.gemfile to select a Gemfile to process");
                            break;
                        }
                        this.gemspec = file;
                    }
                }
            }
        }
        if (this.gemspec == null && this.gemfile == null) {
            getLog().info("no gemspec file or Gemfile. nothing to do.");
            return;
        }
        else {
            File file;
            String type;
            if (this.gemspec == null) {
                file = this.gemfile;
                type = "gemfile";
            }
            else {
                file = this.gemspec;
                type = "gemspec";
            }
            if (!(this.pom.exists() && file.lastModified() > this.pom.lastModified())
                    || this.force) {
                if (this.jrubyVerbose) {
                    getLog().info("create pom using following versions:");
                    getLog().info("\tjruby-plugins-version: "
                            + this.plugin.getVersion());
                    getLog().info("\tjruby-version: " + this.jrubyVersion);
                }
                this.factory.newScriptFromResource("maven/tools/pom_generator.rb")
                        .addArg(type)
                        .addArg(file)
                        .addArg(this.plugin.getVersion())
                        .addArg(this.jrubyVersion)
                        .executeIn(launchDirectory(), this.pom);
            }
            else {
                if (this.jrubyVerbose) {
                    getLog().info("pom is newer then " + type + ". skip creation of pom. force creation with -Dpom.force");
                }
            }
        }
    }
}
