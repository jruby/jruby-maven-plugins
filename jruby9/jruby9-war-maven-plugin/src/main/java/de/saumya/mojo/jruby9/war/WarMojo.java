package de.saumya.mojo.jruby9.war;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.utils.io.IOUtil;
import org.codehaus.plexus.archiver.UnArchiver;

import de.saumya.mojo.jruby9.ArtifactHelper;

/**
 * TODO
 */
@Mojo( name = "war", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true,
requiresDependencyResolution = ResolutionScope.RUNTIME )
public class WarMojo extends org.apache.maven.plugin.war.WarMojo {

    enum Type { ARCHIVE, RUNNABLE, JETTY, UNDERTOW }

    @Parameter( defaultValue = "ARCHIVE", required = true )
    private Type type;

    @Parameter( defaultValue = "de.saumya.mojo.mains.WarMain", required = true )
    private String mainClass;

    @Parameter( defaultValue = "1.7.20", property = "jruby.version", required = true )
    private String jrubyVersion;

    @Parameter( defaultValue = "0.3.0", property = "jruby-mains.version", required = true )
    private String jrubyMainsVersion;

    @Parameter( defaultValue = "1.1.18", property = "jruby-rack.version", required = true )
    private String jrubyRackVersion;

    @Parameter( readonly = true, required = true, defaultValue="${localRepository}" )
    protected ArtifactRepository localRepository;
    
    @Component
    RepositorySystem system;
    
    @Component( hint = "zip" )
    UnArchiver unzip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (type != Type.ARCHIVE) {
            MavenArchiveConfiguration archive = getArchive();
            archive.getManifest().setMainClass(mainClass);
        }
        
        File jrubyWar = new File(getProject().getBuild().getDirectory(), "jrubyWar");
        File output = new File(jrubyWar, "lib");
        ArtifactHelper unzipper = new ArtifactHelper( output,
                unzip, system,
                localRepository, getProject().getRemoteArtifactRepositories());
        unzipper.copy("org.jruby", "jruby-complete", jrubyVersion);
        unzipper.copy("org.jruby.rack", "jruby-rack", jrubyRackVersion);
       
        getProject().getArtifacts().clear();

        Resource[] webResources = getWebResources();
        if (webResources == null) {
            webResources = new Resource[1];
        }
        else {
            webResources = Arrays.copyOf(webResources, webResources.length + 1);
        }
        webResources[webResources.length - 1] = createResource(output.getAbsolutePath(), "WEB-INF/lib");
        setWebResources(webResources);
        
        if (getWebXml() == null) {
            File webXml = new File(jrubyWar, "web.xml");
            try {
                IOUtil.copy(getClass().getClassLoader().getResourceAsStream("web.xml"),
                        new FileOutputStream(webXml));
            } catch (IOException e) {
                throw new MojoExecutionException("could copy web.xml", e);
            }
            if (getLog().isInfoEnabled()) {
                getLog().info("using builtin web.xml: " +
                        webXml.toString().replace(getProject().getBasedir().getAbsolutePath(), ""));
            }
            setWebXml(webXml);
        }
        super.execute();
    }

    protected Resource createResource(String source, String target) {
        Resource resource = new Resource();
        resource.setDirectory(source);
        resource.addInclude("**/*");
        resource.setTargetPath(target);
        return resource;
    }
}