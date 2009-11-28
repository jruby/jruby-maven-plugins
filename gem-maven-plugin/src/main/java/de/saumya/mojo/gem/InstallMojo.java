package de.saumya.mojo.gem;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.jruby.AbstractJRubyMojo;

/**
 * goal to run a local
 * 
 * @goal install
 */
public class InstallMojo extends AbstractJRubyMojo {

    /**
     * @parameter default-value=
     *            "${project.build.directory}/${project.build.finalName}-java.gem"
     */
    private final File gemFile = null;

    public void execute() throws MojoExecutionException {
        execute("-S gem install -l " + this.gemFile.getAbsolutePath());
    }

}
