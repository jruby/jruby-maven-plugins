package de.saumya.mojo.rake;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.Launcher;
import de.saumya.mojo.LauncherFactory;
import de.saumya.mojo.RubyScriptException;
import de.saumya.mojo.gem.AbstractGemMojo;

/**
 * maven wrapper around the rake command.
 * 
 * @goal rake
 * @requiresDependencyResolution test
 */
public class RakeMojo extends AbstractGemMojo {

    private static List<String> NO_CLASSPATH    = Collections.emptyList();

    /**
     * rakefile to be used for the rake command.
     * 
     * @parameter default-value="${rake.file}"
     */
    private final File          rakefile        = null;

    /**
     * output directory for internal use.
     * 
     * @parameter expression="${project.build.directory}"
     * @required
     * @readOnly
     */
    private final File          outputDirectory = null;

    /**
     * ruby script which rakes executes as rakefile.
     * 
     * @parameter default-value="${rake.script}"
     */
    private final String        script          = null;

    /**
     * arguments for the rake command.
     * 
     * @parameter default-value="${rake.args}"
     */
    private final String        rakeArgs        = null;

    /**
     * arguments for the rake command.
     * 
     * @parameter default-value="${args}"
     */
    private final String        args            = null;

    /**
     * rake version used when there is no pom. defaults tp 0.8.7
     * 
     * @parameter default-value="0.8.7" expression="${rake.version}"
     */
    private final String        rakeVersion     = null;

    @Override
    public void execute() throws MojoExecutionException {
        if (this.project.getBasedir() == null) {
            final Artifact artifact = this.artifactFactory.createArtifact("rubygems",
                                                                          "rake",
                                                                          this.rakeVersion,
                                                                          "test",
                                                                          "gem");
            setupGems(artifact);
            final String preRelease = this.rakeVersion != null
                    && this.rakeVersion.matches(".*[a-z][A-Z].*") ? "pre" : "";
            final DefaultArtifactRepository gemsRepo = new DefaultArtifactRepository("rubygems-"
                    + preRelease + "releases",
                    "http://gems.saumya.de/" + preRelease + "releases",
                    new DefaultRepositoryLayout());
            this.remoteRepositories.add(gemsRepo);
        }
        super.execute();
    }

    @Override
    public void executeWithGems() throws MojoExecutionException {
        this.outputDirectory.mkdirs();

        final StringBuilder args = new StringBuilder();
        if (this.script != null) {
            final File scriptFile = new File(this.outputDirectory,
                    "rake_script.rb");
            try {
                final FileWriter writer = new FileWriter(scriptFile);
                writer.write(this.script);
                writer.close();
            }
            catch (final IOException io) {
                throw new MojoExecutionException("error writing temporary script");
            }
            args.append("-f ").append(scriptFile.getAbsolutePath());
        }
        else if (this.rakefile != null) {
            args.append("-f ").append(this.rakefile);
        }
        if (this.rakeArgs != null) {
            args.append(" ").append(this.rakeArgs);
        }
        if (this.args != null) {
            args.append(" ").append(this.args);
        }
        try {
            final Launcher launcher = new LauncherFactory().getEmbeddedLauncher(this.verbose,
                                                                                NO_CLASSPATH,
                                                                                setupEnv(),
                                                                                resolveJRUBYCompleteArtifact().getFile(),
                                                                                this.classRealm);

            launcher.executeScript(launchDirectory(),
                                   binScript("rake").toString(),
                                   args.toString().trim().split("\\s+"));

        }
        catch (final RubyScriptException e) {
            throw new MojoExecutionException("error in rake script", e);
        }
        catch (final DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("could not resolve jruby", e);
        }
        catch (final IOException e) {
            throw new MojoExecutionException("IO error", e);
        }
    }
}
