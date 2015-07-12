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
import de.saumya.mojo.jruby9.Versions;

/**
 * packs a ruby application into war. it can add a launcher to it
 * so it can be executed like runnable jar or it can add an embedded
 * jetty to startup a webserver directly from the war.
 *
 * <li>adds jruby-complete.jar to WEB-INF/lib</li>
 * <li>adds jruby-rack.jar to WEB-INF/lib</li>
 * <li>shaded jruby-mains.jar (for the RUNNABLE case)</li>
 * <li>shaded jetty.jar + its dependencies (for the JETTY case)</li>
 * <li>all declared gems and transitive gems and jars are under WEB-INF/classes</li>
 * <li>all declared jars and transitive jars are under WEB-INF/classes</li>
 * <li>all declared resource are under WEB-INF/classes</li>
 * <li>adds the default resources to WEB-INF/classes</li>
 * 
 * the main class (for RUNNABLE) needs to extract the jruby-complete.jar into a temp directory and 
 * the launcher will set up the GEM_HOME, GEM_PATH and JARS_HOME pointing into the jar (classloader)
 * and takes arguments for executing jruby. any bin stubs from the gem are available via '-S' or any
 * script relative to jar's root can be found as the current directory is inside the jar.
 * 
 * <br/>
 * 
 * embedded JETTY does not take any arguments and will just start up jetty
 * 
 * <br/>
 * 
 * the jruby rack application uses the ClassPathLayout which is designed for this kind of packing
 * the ruby application.
 * 
 * <br/>
 * 
 * default for typical rack application
 * 
 * <li>config.ru</li>
 * <li>lib/**</li>
 * <li>app/**</li>
 * <li>config/**</li>
 * <li>public/**</li>
 *
 * @author christian
 */
@Mojo( name = "war", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true,
requiresDependencyResolution = ResolutionScope.RUNTIME )
public class WarMojo extends org.apache.maven.plugin.war.WarMojo {

    enum Type { ARCHIVE, RUNNABLE, JETTY }

    @Parameter( defaultValue = "ARCHIVE", required = true )
    private Type type;

    @Parameter( required = false )
    private String mainClass;

    @Parameter( required = false, defaultValue = "true" )
    private boolean defaultResource;

    @Parameter( defaultValue = Versions.JRUBY, property = "jruby.version", required = true )
    private String jrubyVersion;

    @Parameter( defaultValue = Versions.JRUBY_MAINS, property = "jruby-mains.version", required = true )
    private String jrubyMainsVersion;

    @Parameter( defaultValue = Versions.JRUBY_RAKE, property = "jruby-rack.version", required = true )
    private String jrubyRackVersion;

    @Parameter( defaultValue = Versions.JETTY, property = "jetty.version", required = true )
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
        case RUNNABLE:
            helper.unzip(jrubyWarClasses, "de.saumya.mojo", "jruby-mains", jrubyMainsVersion);
            if (mainClass == null ) mainClass = "de.saumya.mojo.mains.WarMain";
            
            MavenArchiveConfiguration archive = getArchive();
            archive.getManifest().setMainClass(mainClass);
            
            createAndAddWebResource(jrubyWarClasses, "");
        case ARCHIVE:
        default:
        }
        
        helper.copy(jrubyWarLib, "org.jruby", "jruby-complete", jrubyVersion);
        helper.copy(jrubyWarLib, "org.jruby.rack", "jruby-rack", jrubyRackVersion,
                "org.jruby:jruby-complete"); //exclude jruby-complete
       
        // we bundle jar dependencies the ruby way
        getProject().getArtifacts().clear();

        createAndAddWebResource(jrubyWarLib, "WEB-INF/lib");
        
        if (defaultResource) {
            addCommonRackApplicationResources();
        }

        if (getWebXml() == null) {
            findWebXmlOrUseBuiltin(webXml);
        }
        super.execute();
    }

    private void addCommonRackApplicationResources() {
        Resource resource = new Resource();
        resource.setDirectory(getProject().getBasedir().getAbsolutePath());
        resource.addInclude("config.ru");
        addResource(resource);
        createAndAddResource(new File(getProject().getBasedir(), "lib"));
        createAndAddResource(new File(getProject().getBasedir(), "app"));
        createAndAddResource(new File(getProject().getBasedir(), "public"));
        createAndAddResource(new File(getProject().getBasedir(), "config"));
    }

    private void findWebXmlOrUseBuiltin(File webXml)
            throws MojoExecutionException {
        // TODO search web.xml
        try {
            IOUtil.copy(getClass().getClassLoader().getResourceAsStream("web.xml"),
                    new FileOutputStream(webXml));
        } catch (IOException e) {
            throw new MojoExecutionException("could copy web.xml", e);
        }
        if (getLog().isInfoEnabled()) {
            getLog().info("using builtin web.xml: " +
                    webXml.toString().replace(getProject().getBasedir().getAbsolutePath() + File.separatorChar, ""));
        }
        setWebXml(webXml);
    }

    private void createAndAddResource(File source){
        getProject().addResource(createResource(source.getAbsolutePath(), null));
    }

    private void createAndAddWebResource(File source, String target){
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
        resource.addExclude("META-INF/**/*");
        resource.addExclude("WEB-INF/**/*");
        resource.addExclude("**/web.xml");
        if (target != null) resource.setTargetPath(target);
        return resource;
    }
}