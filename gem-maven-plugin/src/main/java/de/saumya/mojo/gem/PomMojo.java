package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.util.FileUtils;

import de.saumya.mojo.jruby.AbstractJRubyMojo;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * goal to converts a gemspec file into pom.xml.
 * 
 * @goal pom
 */
public class PomMojo extends AbstractJRubyMojo {

    /** @parameter expression="${plugin}" @readonly */
    PluginDescriptor  plugin;

    /**
     * the pom file to generate
     * <br/>
     * Command line -Dpom=...
     * 
     * @parameter expression="${pom}" default-value="pom.xml"
     */
    protected File    pom;

    /**
     * force overwrite of an existing pom
     * <br/>
     * Command line -Dpom.force=...
     * 
     * @parameter default-value="${pom.force}"
     */
    protected boolean force = false;

    /**
     * temporary store generated pom.
     * 
     * @parameter default-value="${project.build.drectory}/pom.xml"
     */
    protected File tmpPom;

    /**
     * use a gemspec file to generate a pom
     * <br/>
     * Command line -Dpom.gemspec=...
     * 
     * @parameter default-value="${pom.gemspec}"
     */
    protected File    gemspec;

    /**
     * use Gemfile to generate a pom
     * <br/>
     * Command line -Dpom.gemfile=...
     * 
     * @parameter expression="${pom.gemfile}"
     *            default-value="Gemfile"
     */
    protected File    gemfile;
    
    /**
     * @parameter
     */
    private boolean skipGeneration;

    @Override
    public void executeJRuby() throws MojoExecutionException, ScriptException, IOException {
        File source = null;
        if( this.skipGeneration )
        {
            this.gemfile = null;
            this.gemspec = null;
        }
        else
        {
            if ( this.gemfile.exists() )
            {
                this.gemspec = null;
            }
            else
            {
                this.gemfile = null;
                if (this.gemspec == null)
                {
                    this.gemspec = findGemspec();
                }
            }

            if (this.gemspec != null)
            {
                generatePom( this.gemspec, "gemspec" );
                source = this.gemspec;
            }
            if (this.gemfile != null )
            {
                generatePom( this.gemfile,  "gemfile" );
                source = this.gemspec;
            }
        }

        copyGeneratedPom( source );        
    }

    private void copyGeneratedPom( File source ) throws IOException {
        if( pom.exists() && tmpPom.exists() )
        {
            String pomString = FileUtils.fileRead( pom );
            String tmpString = FileUtils.fileRead( tmpPom );
            if ( force ||
                 // no source then copy on change
                 ( source == null && ! pomString.equals( tmpString ) ) ||
                 // with source copy on modification
                 ( source != null && source.lastModified() > pom.lastModified() ) )
            {
                movePom();
            }
            else if (this.jrubyVerbose)
            {
                if ( source != null )
                {
                    getLog().info( "skip creation of pom. force creation with -Dpom.force");
                }
                else
                {
                    tmpPom.delete();
                    getLog().info( "generated pom up to date - deleted " +
                            tmpPom.getAbsolutePath().replace( this.project.getBasedir().getAbsolutePath() + "/", "" ) );
                }
            }
        }
        else if ( tmpPom.exists() )
        {
            movePom();
        }
    }

    private void movePom() throws IOException {
        //pom.delete();
        FileUtils.rename( tmpPom, pom );
        // helper for ruby-maven to keep the project data valid 
        if (project.getFile().getAbsolutePath().equals( tmpPom.getAbsolutePath() ) ){
            project.setFile( pom );
        }
        if (this.jrubyVerbose)
        {
          getLog().info( "moved " +
                         tmpPom.getAbsolutePath().replace( this.project.getBasedir().getAbsolutePath() + "/", "" ) + 
                         " to " + 
                         pom.getAbsolutePath().replace( this.project.getBasedir().getAbsolutePath() + "/", "" ) );
        }
    }

    private void generatePom(File file, String type) throws ScriptException,
            IOException {
        this.factory.newScriptFromResource("maven/tools/pom_generator.rb")
                .addArg(type)
                .addArg(file)
                .addArg(this.plugin.getVersion())
                .addArg(this.jrubyVersion)
                .executeIn(launchDirectory(), this.tmpPom);
    }

    private File findGemspec() {
        getLog().debug("no gemspec file given, see if there is single one");
        File result = null;
        File basedir = this.project.getBasedir() == null
                ? new File(".")
                : this.project.getBasedir();
        for (final File file : basedir.listFiles()) {
            if (file.getName().endsWith(".gemspec")) {
                if (result != null) {   
                    getLog().info("there is no gemspec file given but there are more then one in the current directory.");
                    getLog().info("use '-Dpom.gemspec=...' to select the gemspec file or -Dpom.gemfile to select a Gemfile to process");
                    return null;
                }
                result = file;
            }
        }
        return result;
    }
}
