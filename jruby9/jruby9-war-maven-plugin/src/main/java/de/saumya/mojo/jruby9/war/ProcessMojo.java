package de.saumya.mojo.jruby9.war;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import de.saumya.mojo.jruby9.AbstractProcessMojo;

/**
 * TODO
 */
@Mojo( name = "process", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresProject = true,
       threadSafe = true )
public class ProcessMojo extends AbstractProcessMojo {   
}