package de.saumya.mojo.rails3;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.codehaus.plexus.util.FileUtils;

/**
 * goal to run the rails console
 * 
 * @goal war
 * @phase process-resources
 */
public class WarMojo extends AbstractRailsMojo {

    /**
     * files from the public directory or other directories to be copy into
     * webapps folder. default:
     * 
     * <pre>
     * <publicFiles>
     *   <fileset>
     *     <directory>public</directory>
     *     <excludes>
     *       <exclude>WEB-INF/**</exclude>
     *       <exclude>&#2A;&#2A;/.git*</exclude>
     *     </excludes>
     *     <useDefaultExcludes>true</useDefaultExcludes>
     *   </fileset>
     * </publicFiles>
     * </pre>
     * 
     * @parameter name="public"
     */
    protected List<Fileset> publicFiles      = null;

    /**
     * 
     * @parameter name="application"
     */
    protected List<Fileset> applicationFiles = null;

    /**
     * @parameter 
     *            expression="${project.build.directory}/${project.build.finalName}"
     */
    protected File          buildDirectory;

    @Override
    protected void executeWithGems() throws MojoExecutionException {
        if (this.publicFiles == null) {
            final Fileset publicFileset = newFileset("public");
            publicFileset.addExclude("WEB-INF");
            this.publicFiles = new ArrayList<Fileset>();
            this.publicFiles.add(publicFileset);
        }
        copyFilesets(this.publicFiles, this.buildDirectory);

        if (this.applicationFiles == null) {
            this.applicationFiles = new ArrayList<Fileset>();
            final Fileset files = newFileset(".");
            files.addInclude("app/**");
            files.addInclude("vendor/**");
            files.addInclude("config/**");
            files.addInclude("lib/**");
            this.applicationFiles.add(files);
            // this.applicationFiles.add(newFileset("vendor"));
            // this.applicationFiles.add(newFileset("config"));
            // this.applicationFiles.add(newFileset("lib"));
        }
        copyFilesets(this.applicationFiles, new File(this.buildDirectory,
                "WEB-INF"));

        final List<Fileset> gemFiles = new ArrayList<Fileset>();
        final String gemPath = this.gemPath.getPath();
        final int index = gemPath.startsWith(launchDirectory().getPath())
                ? launchDirectory().getPath().length() + 1
                : 0;
        final Fileset gems = newFileset(gemPath.substring(index));
        gems.addExclude("bin/**");
        gems.addExclude("doc/**");
        gems.addExclude("cache/**");
        gems.addExclude("**/test/**");
        gems.addExclude("**/spec/**");

        gemFiles.add(gems);
        copyFilesets(gemFiles, new File(this.buildDirectory, "WEB-INF/gems"));
    }

    private Fileset newFileset(final String directory) {
        final Fileset fileset = new Fileset();
        fileset.setDirectory(directory);
        fileset.addExclude("**/.git*");
        fileset.setUseDefaultExcludes(true);
        return fileset;
    }

    private void copyFilesets(final List<Fileset> files, final File destination)
            throws MojoExecutionException {
        final FileSetManager fileSetManager = new FileSetManager(getLog());
        for (final Fileset fileset : files) {
            getLog().debug("copy from '" + fileset.getDirectory() + "': "
                    + Arrays.toString(fileSetManager.getIncludedFiles(fileset)));
            final File launchDirectory = new File(launchDirectory(),
                    fileset.getDirectory());
            for (final String file : fileSetManager.getIncludedFiles(fileset)) {
                try {
                    FileUtils.copyFile(new File(launchDirectory, file),
                                       new File(destination, file));
                }
                catch (final IOException e) {
                    throw new MojoExecutionException("error copying file "
                            + file, e);
                }
            }
        }
    }
}
