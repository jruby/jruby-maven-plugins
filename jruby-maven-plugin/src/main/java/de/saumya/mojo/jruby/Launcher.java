package de.saumya.mojo.jruby;

import java.io.File;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;

interface Launcher {

    public abstract void execute(final File launchDirectory,
            final String[] args, final Set<Artifact> artifacts,
            final Artifact jrubyArtifact, File classesDirectory)
            throws MojoExecutionException,
            DependencyResolutionRequiredException;

}