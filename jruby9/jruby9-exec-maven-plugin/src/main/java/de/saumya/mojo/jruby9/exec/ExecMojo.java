package de.saumya.mojo.jruby9.exec;

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import de.saumya.mojo.jruby9.AbstractJRuby9Mojo;
import de.saumya.mojo.jruby9.JarDependencies;
import de.saumya.mojo.jruby9.JarDependencies.Filter;
import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * executes a ruby script in context of the gems from pom. the arguments for
 * jruby are build like this:
 * <code>${jruby.args} ${exec.file} ${exec.args} ${args}</code> <br/>
 * to execute an inline script the exec parameters are ignored.
 * 
 */
@Mojo( name = "exec", requiresProject = true, threadSafe = true,
       requiresDependencyResolution = ResolutionScope.TEST )
public class ExecMojo extends AbstractJRuby9Mojo {

    /**
     * ruby code from the pom configuration part which gets executed.
     */
    @Parameter(property = "exec.script")
    protected String script   = null;

    /**
     * ruby file which gets executed in context of the given gems..
     */
    @Parameter(property = "exec.file")
    protected File   file     = null;

    /**
     * ruby file found on search path which gets executed. the search path
     * includes the executable scripts which were installed via the given
     * gem-artifacts.
     */
    @Parameter(property = "exec.command")
    protected String command = null;

    /**
     * output file where the standard out will be written
     */
    @Parameter(property = "exec.outputFile")
    protected File outputFile = null;

    /**
     * arguments separated by whitespaces for the ruby script given through file parameter.
     * no quoting or escaping possible - if needed use execArglines instead.
     */
    @Parameter(property = "exec.args")
    protected String execArgs = null;

    /**
     * an array of arguments which can contain spaces for the ruby script given through file parameter.
     */
    @Parameter
    protected String[] execArgLines = null;

    /**
     * add project test class path to JVM classpath.
     */
     /* we want the opposite default here than the superclass */
    @Parameter(property = "gem.addProjectClasspath", defaultValue = "false")
    protected boolean addProjectClasspath;


    @Override
    protected void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException {
        JarDependencies jars = new JarDependencies(project.getBuild().getDirectory(),
                "Jars_" + plugin.getGoalPrefix() + ".lock");
        jars.addAll(plugin.getArtifacts(), new Filter(){

            @Override
            public boolean addIt(Artifact a) {
                return a.getScope().equals("runtime") &&
                        !project.getArtifactMap().containsKey(a.getGroupId() +":" + a.getArtifactId());
            }
            
        });

        jars.addAll(project.getArtifacts());
        jars.generateJarsLock();
        
        factory.addEnv("JARS_HOME", localRepository.getBasedir());
        factory.addEnv("JARS_LOCK", jars.lockFilePath());
        factory.addSwitch("-r", "jars/setup");

        Script s;
        if (this.script != null && this.script.length() > 0) {
            s = this.factory.newScript(this.script);
        }
        else if (this.file != null) {
            s = this.factory.newScript(this.file);
        }
        else if (this.command != null) {
            s = this.factory.newScriptFromSearchPath( this.command );
        }
        else {
            s = this.factory.newArguments();
        }
        if ( execArgLines != null ){
            for( String arg: execArgLines ){
                s.addArg( arg );
            }
        }
        s.addArgs(this.execArgs);
        s.addArgs(this.args);
        if (s.isValid()) { 
            if(outputFile != null){
                s.executeIn(launchDirectory(), outputFile);
            }
            else {
                s.executeIn(launchDirectory());
            }
        }
        else {
            getLog().warn("no arguments given. for more details see: mvn ruby:help -Ddetail -Dgoals=exec");
        }
    }
}
