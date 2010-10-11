package de.saumya.mojo.rails3;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * goal to run rails command with the given arguments. either to generate a
 * fresh rails application or to run the rails script from within a rails
 * application.
 * 
 * DEPRECATED: use NewMojo instead
 * 
 * @goal rails
 */
@Deprecated
public class RailsMojo extends NewMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().warn("rails mojo is deprecated. use 'new' instead");
        super.execute();
    }
}
