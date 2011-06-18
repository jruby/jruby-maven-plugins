package de.saumya.mojo.bundler;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.sonatype.aether.RepositorySystemSession;

import de.saumya.mojo.gem.AbstractGemMojo;
import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * maven wrapper around the bundler install command.
 * 
 * @goal install
 * @phase initialize
 * @requiresDependencyResolution test
 */
public class InstallMojo extends AbstractGemMojo {

    /**
     * arguments for the bundler command.
     * 
     * @parameter default-value="${bundler.args}"
     */
    private final String            bundlerArgs    = null;

    /**
     * bundler version used when there is no pom. defaults to latest version.
     * 
     * @parameter default-value="${bundler.version}"
     */
    private final String            bundlerVersion = null;

    /**
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    private RepositorySystemSession repoSession;

    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException, GemException {
        if(project.getFile() != null){
            File bundlerTouch = new File(project.getBuild().getDirectory(), "bundler-lastrun-timestamp");
            if (bundlerTouch.exists() && bundlerTouch.lastModified() > project.getFile().lastModified()) {
                getLog().debug("skip bundler install since pom did not change since last run");
                return;
            }
            else {
                FileUtils.fileWrite(bundlerTouch.getAbsolutePath(), "");
            }
        }
        final Script script = this.factory.newScriptFromSearchPath("bundle");
        script.addArg("install");
        if (this.project.getBasedir() == null) {

            this.gemsInstaller.installGem("bundler",
                                          this.bundlerVersion,
                                          this.repoSession,
                                          this.localRepository);

        }
        else {
            script.addArg("--quiet");
            script.addArg("--local");
        }
        if (this.bundlerArgs != null) {
            script.addArgs(this.bundlerArgs);
        }
        if (this.args != null) {
            script.addArgs(this.args);
        }

        script.executeIn(launchDirectory());
    }
}
