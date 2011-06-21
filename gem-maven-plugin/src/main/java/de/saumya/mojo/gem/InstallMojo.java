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
 * @phase install
 */
public class InstallMojo extends AbstractGemMojo {

    /**
     * arguments for the "gem install" command.
     * <br/>
     * Command line -Dinstall.args=...
     * 
     * @parameter default-value="${install.args}"
     */
    protected String installArgs = null;

    /**
     * gem file to install locally.<br/>
     * <b>Note:</b> this will install the gem in ${gem.home} so in general that is only
     * useful if some other goal does something with it
     * <br/>
     * Command line -Dgem=...
     * 
     * @parameter default-value="${gem}"
     */
    protected File   gem         = null;

    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException, MojoFailureException {
        final Script script = this.factory.newScriptFromJRubyJar("gem")
                .addArg("install");
        // no given gem and pom artifact in place
        if (this.gem == null && this.project.getArtifact() != null
                && this.project.getArtifact().getFile() != null
                && this.project.getArtifact().getFile().exists()) {
            final GemArtifact gemArtifact = new GemArtifact(this.project);
            // skip artifact unless it is a gem.
            // this allows to use this mojo for installing arbitrary gems 
            // via the args parameter
            if (gemArtifact.isGem()) {
                script.addArg("-l", gemArtifact.getFile());
            }
        }
        else {
            // no pom artifact and no given gem so search for a gem
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
            }
            if (this.gem != null) {
                getLog().info("use gem: " + this.gem);
                script.addArg("-l", this.gem);
            }
        }
        script.addArg((installRDoc ? "--" : "--no-") + "rdoc")
                .addArg((installRI ? "--" : "--no-") + "ri")
                .addArgs(this.installArgs)
                .addArgs(this.args)
                .execute();
    }
}
