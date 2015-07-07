package de.saumya.mojo.jruby9;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * generates ".jrubydir" files for all resource and gems to allow jruby
 * to perform directory globs inside the jar.
 *  
 * @author christian
 *
 */
public abstract class AbstractProcessMojo extends AbstractMojo {

    @Parameter( defaultValue = "${project.build.outputDirectory}", readonly = true )
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        Path root = outputDirectory.toPath();
        try {
            Files.walkFileTree(root, new JRubyDirectory(root));
        } catch (IOException e) {
            throw new MojoExecutionException("could not generate .jrubydir files", e);
        }
    }
}