package de.saumya.mojo.gem;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.jruby.AbstractJRubyMojo;

/**
 * goal to locally install a given gem
 * 
 * @goal install
 */
public class InstallMojo extends AbstractJRubyMojo {

    /**
     * @parameter expression="${project.artifact}"
     */
    private final Artifact artifact = null;

    /**
     * arguments for the gem command of JRuby.
     * 
     * @parameter default-value="${gem.install}"
     */
    protected String       args     = null;

    public void execute() throws MojoExecutionException {
        if (this.artifact != null) {
            final GemArtifact gemArtifact = new GemArtifact(this.project);
            execute("-S gem install -l " + gemArtifact.getFile());
        }
        else {
            String commandString = "-S gem install";
            if (this.args != null) {
                commandString += " " + this.args;
            }
            execute(commandString);
        }

    }
}
