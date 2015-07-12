package de.saumya.mojo.jruby9.jar;

import java.io.File;
import java.lang.reflect.Field;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.archiver.UnArchiver;

import de.saumya.mojo.jruby9.ArtifactHelper;
import de.saumya.mojo.jruby9.Versions;

/**
 * packs a ruby application into runnable jar.
 *
 * <li>shaded jruby-complete.jar</li>
 * <li>shaded jruby-mains.jar</li>
 * <li>all declared gems and transitive gems and jars</li>
 * <li>all declared jars and transitive jars</li>
 * <li>all declared resource</li>
 * 
 * the main class sets up the GEM_HOME, GEM_PATH and JARS_HOME and takes arguments
 * for executing jruby. any bin stubs from the gem are available via '-S' or any
 * script relative to jar's root can be found as the current directory is inside the jar.
 * 
 * <br/>
 * 
 * if there is a 'jar-bootstrap.rb' in the root of the jar, then the default main class will
 * execute this script and pass all the arguments to bootstrap script.
 * 
 * @author christian
 */
@Mojo( name = "jar", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true,
       requiresDependencyResolution = ResolutionScope.RUNTIME )
public class JarMojo extends org.apache.maven.plugin.jar.JarMojo {

    @Parameter( defaultValue = "de.saumya.mojo.mains.JarMain", required = true )
    private String mainClass;

    @Parameter( defaultValue = Versions.JRUBY, property = "jruby.version", required = true )
    private String jrubyVersion;

    @Parameter( defaultValue = Versions.JRUBY_MAINS, property = "jruby-mains.version", required = true )
    private String jrubyMainsVersion;

    @Parameter( readonly = true, defaultValue="${localRepository}" )
    protected ArtifactRepository localRepository;
    
    @Component
    RepositorySystem system;
    
    @Component( hint = "zip" )
    UnArchiver unzip;

    @Override
    public void execute() throws MojoExecutionException {
        MavenArchiveConfiguration archive = getArchive();
        archive.getManifest().setMainClass(mainClass);

        ArtifactHelper helper = new ArtifactHelper(unzip, system,
                localRepository, getProject().getRemoteArtifactRepositories());
        File output = new File( getProject().getBuild().getOutputDirectory());
                
        helper.unzip(output, "org.jruby", "jruby-complete", jrubyVersion);
        helper.unzip(output, "de.saumya.mojo", "jruby-mains", jrubyMainsVersion);
       
        super.execute();
    }

    private MavenArchiveConfiguration getArchive() throws MojoExecutionException {
        try {
             Field archiveField = getClass().getSuperclass().getSuperclass().getDeclaredField("archive");
             archiveField.setAccessible(true);
             return (MavenArchiveConfiguration) archiveField.get(this);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new MojoExecutionException("can not use reflection", e);
        }
    }
}