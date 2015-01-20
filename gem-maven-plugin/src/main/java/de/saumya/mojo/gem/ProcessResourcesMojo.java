package de.saumya.mojo.gem;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * installs a set of given gems without resolving any transitive dependencies
 * 
 * @goal process-resources
 * @phase process-resources
 */
public class ProcessResourcesMojo extends AbstractGemMojo {

    /** @parameter  */
    protected List<String> includeRubyResources;

    /** @parameter  */
    protected List<String> excludeRubyResources;

    @Override
    protected void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException, GemException {
        if ( includeRubyResources != null) {
            DirectoryScanner scanner = scan();
            processBaseDirectory(scanner);
            processNestedDiretories(scanner);
        }    
    }

    private void processNestedDiretories(DirectoryScanner scanner) throws IOException, ScriptException {
        String[] directories = scanner.getIncludedDirectories();   
        if (directories.length > 0) {
            StringBuilder script = new StringBuilder("require 'jruby/commands';");
            for( String dir: directories) {
                System.err.println(dir);
                if (!dir.contains("/")) {   
                    script.append("JRuby::Commands.generate_dir_info('" +
                           new File( project.getBuild().getOutputDirectory(), dir ).getAbsolutePath() + "', false);");
                }
            }
            Script s = this.factory.newScript(script.toString());
            s.execute();
        }
    }

    private void processBaseDirectory(DirectoryScanner scanner)
            throws IOException {
        String[] files = scanner.getIncludedFiles();
        if (files.length > 0) {
            StringBuilder fileList = new StringBuilder(".\n");
            for (String file: files) {
                if (!file.contains("/")) {
                    fileList.append(file).append("\n");
                }
            }
            FileUtils.fileWrite(new File(project.getBuild().getOutputDirectory(), ".jrubydir"), fileList.toString());
        }
    }

    private DirectoryScanner scan() {
        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(project.getBuild().getOutputDirectory());
        scanner.setIncludes(includeRubyResources.toArray(new String[includeRubyResources.size()]));

        if ( excludeRubyResources != null )
        {
            scanner.setIncludes(excludeRubyResources.toArray(new String[excludeRubyResources.size()]));
        }
        scanner.scan();
        return scanner;
    }
}
