package de.saumya.mojo.jruby9.exec;

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

import de.saumya.mojo.gem.AbstractGemMojo;
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
 * @goal exec
 * @requiresDependencyResolution test
 */
public class ExecMojo extends AbstractGemMojo {

    /**
     * ruby code from the pom configuration part which gets executed.
     * <br/>
     * Command line -Dexec.script=...
     * 
     * @parameter expression="${exec.script}"
     */
    protected String script   = null;

    /**
     * ruby file which gets executed in context of the given gems..
     * <br/>
     * Command line -Dexec.file=...
     * 
     * @parameter expression="${exec.file}"
     */
    protected File   file     = null;

    /**
     * ruby file found on search path which gets executed. the search path
     * includes the executable scripts which were installed via the given
     * gem-artifacts.
     * <br/>
     * Command line -Dexec.command=...
     *
     * @parameter expression="${exec.command}"
     */
    protected String command = null;

    /**
     * output file where the standard out will be written
     * <br/>
     * Command line -Dexec.outputFile=...
     * 
     * @parameter expression="${exec.outputFile}"
     */
    protected File outputFile = null;

    /**
     * arguments separated by whitespaces for the ruby script given through file parameter.
     * no quoting or escaping possible - if needed use execArglines instead.
     * <br/>
     * Command line -Dexec.args=...
     * 
     * @parameter expression="${exec.args}"
     */
    protected String execArgs = null;

    /**
     * an array of arguments which can contain spaces for the ruby script given through file parameter.
     * 
     * @parameter
     */
    protected String[] execArgLines = null;

    /**
     * add project test class path to JVM classpath.
     * @parameter default-value=false expression="${gem.addProjectClasspath}"
     */
     /* we want the opposite default here than the superclass */
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
