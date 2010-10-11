package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;

import de.saumya.mojo.gems.GemspecConverter;
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
     * @parameter expression="${pom}" default-value="pom.xml"
     */
    protected File    pom;

    /**
     * @parameter default-value="${pom.force}"
     */
    protected boolean force = false;

    /**
     * @parameter default-value="${pom.gemspec}"
     */
    protected File    gemspecFile;

    @Override
    public void executeJRuby() throws MojoExecutionException {
        if (this.pom.exists() && !this.force) {
            getLog().info(this.pom.getName()
                    + " already exists. use '-Dpom.force=true' to overwrite");
            return;
        }
        if (this.gemspecFile == null) {
            getLog().debug("no gemspec file given, see if there is single one");
            for (final File file : new File(".").listFiles()) {
                if (file.getName().endsWith(".gemspec")) {
                    if (this.gemspecFile != null) {
                        getLog().info("there is no gemspec file given but there are more then one in the current directory.");
                        getLog().info("use '-Dpom.gemspec=...' to select the gemspec file to process");
                        return;
                    }
                    this.gemspecFile = file;
                }
            }
        }
        if (this.gemspecFile == null) {
            getLog().info("there is no gemspec file given and no gemspec file found (*.gemspec). nothing to do.");
            return;
        }
        else {
            try {
                final GemspecConverter gemspec = new GemspecConverter(this.logger,
                        this.factory);
                gemspec.createPom(this.gemspecFile, this.plugin.getVersion(), this.pom);

            }
            catch (final ScriptException e) {
                throw new MojoExecutionException("error in script", e);
            }
            catch (final IOException e) {
                throw new MojoExecutionException("IO error", e);
            }
        }
    }
}
