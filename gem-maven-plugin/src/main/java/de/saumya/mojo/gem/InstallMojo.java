package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * goal to locally install a given gem
 * 
 * @goal install
 */
public class InstallMojo extends AbstractGemMojo {

    /**
     * arguments for the "gem install" command.
     * 
     * @parameter default-value="${install.orgs}"
     */
    protected String installArgs = null;

    /**
     * gem file to install locally.
     * 
     * @parameter default-value="${gem}"
     */
    protected File   gem         = null;

    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException, MojoFailureException {
        final Script script = this.factory.newScriptFromResource(GEM_RUBY_COMMAND)
                .addArg("install");
        // TODO if artifact is set, check on an existing gem in target
        if (this.project.getArtifact() != null
                && this.project.getArtifact().getFile() != null
                && this.project.getArtifact().getFile().exists()) {
            final GemArtifact gemArtifact = new GemArtifact(this.project);
            script.addArgs(this.installArgs);
            script.addArgs(this.args);
            script.addArg("-l", gemArtifact.getFile());
            script.execute();
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
                    script.addArg("-l", this.gem);
                    // commandString += " -l " + this.gem.getAbsolutePath();
                }
            }
            script.addArgs(this.installArgs);
            script.addArgs(this.args);
            script.execute();
        }
    }
}
