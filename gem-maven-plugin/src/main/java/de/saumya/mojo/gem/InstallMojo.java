package de.saumya.mojo.gem;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

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
     * arguments for the "gem install" command.
     * 
     * @parameter default-value="${gem.install}"
     */
    protected String       args     = null;

    /**
     * gem file to install locally.
     * 
     * @parameter default-value="${gem}"
     */
    protected File         gem      = null;

    public void execute() throws MojoExecutionException, MojoFailureException {
        String commandString = "-S gem install";
        // TODO if artifact is set, check on an existing gem in target
        if (this.artifact != null && this.artifact.getFile() != null
                && this.artifact.getFile().exists()) {
            final GemArtifact gemArtifact = new GemArtifact(this.project);
            commandString += " -l " + gemArtifact.getFile();
        }
        else {
            if (this.gem == null) {
                for (final File f : this.launchDirectory().listFiles()) {
                    if (f.getName().endsWith(".gem")) {
                        if (this.gem == null) {
                            this.gem = f;
                        }
                        else {
                            throw new MojoFailureException("more than one gem file found, use -Dgem=... to specifiy one");
                        }
                    }
                }
                if (this.gem != null) {
                    getLog().info("use gem: " + this.gem);
                    commandString += " -l " + this.gem.getAbsolutePath();
                }
            }
            if (this.args != null) {
                commandString += " " + this.args;
            }
        }
	execute(commandString, false);
    }

    // @Override
    // protected File launchDirectory() {
    // return this.launchDir.getAbsoluteFile();
    // }

}
