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

    enum Type { ARCHIVE, RUNNABLE, JETTY }//, UNDERTOW }

    @Parameter( defaultValue = "ARCHIVE", required = true )
    private Type type;

    @Parameter( required = false )
    private String mainClass;

    @Parameter( defaultValue = "1.7.20", property = "jruby.version", required = true )
    private String jrubyVersion;

    @Parameter( defaultValue = "0.3.0", property = "jruby-mains.version", required = true )
    private String jrubyMainsVersion;

    @Parameter( defaultValue = "1.1.18", property = "jruby-rack.version", required = true )
    private String jrubyRackVersion;

    @Parameter( defaultValue = "8.1.14.v20131031", property = "jetty.version", required = true )
    private String jettyVersion;
    
    @Parameter( readonly = true, required = true, defaultValue="${localRepository}" )
    protected ArtifactRepository localRepository;
    
    @Component
    RepositorySystem system;
    
    @Component( hint = "zip" )
    UnArchiver unzip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ArtifactHelper helper = new ArtifactHelper(unzip, system,
                localRepository, getProject().getRemoteArtifactRepositories());
        File jrubyWar = new File(getProject().getBuild().getDirectory(), "jrubyWar");
        File jrubyWarLib = new File(jrubyWar, "lib");
        File webXml = new File(jrubyWar, "web.xml");
        File jrubyWarClasses = new File(jrubyWar, "classes");
        
        switch(type) {
        case JETTY:
            helper.unzip(jrubyWarClasses, "org.eclipse.jetty", "jetty-server", jettyVersion);
            helper.unzip(jrubyWarClasses, "org.eclipse.jetty", "jetty-webapp", jettyVersion);
            if (mainClass == null ) mainClass = "de.saumya.mojo.mains.JettyRunMain";
//        case UNDERTOW:
//            //helper.unzip(jrubyWarClasses, "de.saumya.mojo", "jruby-mains", jettyVersion);
//            if (mainClass == null ) mainClass = "de.saumya.mojo.mains.UndertowMain";
        case RUNNABLE:
            helper.unzip(jrubyWarClasses, "de.saumya.mojo", "jruby-mains", jrubyMainsVersion);
            if (mainClass == null ) mainClass = "de.saumya.mojo.mains.WarMain";
            
            MavenArchiveConfiguration archive = getArchive();
            archive.getManifest().setMainClass(mainClass);
            
            createAndAddResource(jrubyWarClasses, "");
        case ARCHIVE:
        default:
        }
        
        helper.copy(jrubyWarLib, "org.jruby", "jruby-complete", jrubyVersion);
        helper.copy(jrubyWarLib, "org.jruby.rack", "jruby-rack", jrubyRackVersion, "org.jruby:jruby-complete");
       
        // we bundle jar dependencies the ruby way
        getProject().getArtifacts().clear();

        createAndAddResource(jrubyWarLib, "WEB-INF/lib");
        
        if (getWebXml() == null) {
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

    private void createAndAddResource(File source, String target){
        addResource(createResource(source.getAbsolutePath(), target));
    }

    private void addResource(Resource resource) {
        Resource[] webResources = getWebResources();
        if (webResources == null) {
            webResources = new Resource[1];
        }
        else {
            webResources = Arrays.copyOf(webResources, webResources.length + 1);
        }
        webResources[webResources.length - 1] = resource;
        setWebResources(webResources);
    }

    protected Resource createResource(String source, String target) {
        Resource resource = new Resource();
        resource.setDirectory(source);
        resource.addInclude("**/*");
        resource.addExclude("jetty.css");
        resource.addExclude("about.html");
        resource.addExclude("about_files/*");
        resource.addExclude("META-INF/*");
        resource.setTargetPath(target);
        return resource;
    }
}