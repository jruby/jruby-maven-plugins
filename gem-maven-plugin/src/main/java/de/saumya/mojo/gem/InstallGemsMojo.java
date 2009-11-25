package de.saumya.mojo.gem;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.jruby.AbstractJRubyMojo;

/**
 * goal to run gem install
 * 
 * @goal install
 */
public class InstallGemsMojo extends AbstractJRubyMojo {
    /**
     * list of comma separated gem names.
     * 
     * @parameter default-value="${jruby.gems}"
     */
    private final String gems = null;

    public void execute() throws MojoExecutionException {
        if (this.gems != null) {
            ensureGems(this.gems.split("[,]"));
        }
        else {
            getLog().warn("no gems argument given. use -Djruby.gems=...");
        }
    }
}
