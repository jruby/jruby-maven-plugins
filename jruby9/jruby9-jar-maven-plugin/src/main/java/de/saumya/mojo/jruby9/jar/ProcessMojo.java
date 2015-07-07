package de.saumya.mojo.jruby9.jar;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import de.saumya.mojo.jruby9.AbstractProcessMojo;

/**
 * generates ".jrubydir" files for all resource and gems to allow jruby
 * to perform directory globs inside the jar.
 *  
 * @author christian
 * 
 */
@Mojo( name = "process", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresProject = true,
       threadSafe = true )
public class ProcessMojo extends AbstractProcessMojo {
}