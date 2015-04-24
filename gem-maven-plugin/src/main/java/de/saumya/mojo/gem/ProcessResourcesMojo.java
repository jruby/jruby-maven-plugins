package de.saumya.mojo.gem;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
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
    protected List<String> excludeRubyResources = Collections.emptyList();
    
    @Override
    protected void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException, GemException {
        File jrubydir = new File(project.getBuild().getOutputDirectory(), ".jrubydir");
        if ( includeRubyResources != null ) {
            jrubydir.delete();
            DirectoryScanner scanner = scan(includeRubyResources.toArray(new String[includeRubyResources.size()]),
                    excludeRubyResources.toArray(new String[excludeRubyResources.size()])      );
            processBaseDirectory(scanner, jrubydir);
            processNestedDiretories(scanner);
        }
        if ( rubySourceDirectory.exists() ) {
            processDir(jrubydir, rubySourceDirectory);            
        }
        if ( libDirectory.exists() && includeLibDirectoryInResources ) {
            processDir(jrubydir, libDirectory);            
        }
    }

    private void processDir(File jrubydir, File dir) throws IOException,
            ScriptException {
        File[] dirs = dir.listFiles(new FileFilter() {
            
            public boolean accept(File f) {
                return f.isDirectory();
            }
        });
        String[] includes = new String[ dirs.length + 1 ];
        includes[ 0 ] =  "*";
        int index = 1;
        for( File d: dirs ) {
            includes[ index ++ ] = d.getName() + "/*";
        }
        DirectoryScanner scanner = scan(includes, new String[0]);
         
        processBaseDirectory(scanner, jrubydir);
        processNestedDiretories(scanner);
    }

    private void processNestedDiretories(DirectoryScanner scanner) throws IOException, ScriptException {
        String[] directories = scanner.getIncludedDirectories();   
        if (directories.length > 0) {
            StringBuilder script = new StringBuilder("require 'jruby/commands';");
            for( String dir: directories) {
                if (!dir.contains("/")) {   
                    script.append("JRuby::Commands.generate_dir_info('" +
                           new File( project.getBuild().getOutputDirectory(), dir ).getAbsolutePath() + "', false) if JRuby::Commands.respond_to? :generate_dir_info;" );
                }
            }
            Script s = this.factory.newScript(script.toString());
            s.execute();
        }
    }

    private void processBaseDirectory(DirectoryScanner scanner, File jrubydir)
            throws IOException {
        String[] files = scanner.getIncludedFiles();
        if (files.length > 0) {
            StringBuilder fileList;
            if (jrubydir.exists()) {
                fileList = new StringBuilder(FileUtils.fileRead(jrubydir));
            }
            else {
                fileList = new StringBuilder(".\n");
            }
            for (String file: files) {
                if (!file.contains("/")) {
                    fileList.append(file).append("\n");
                }
            }
            FileUtils.fileWrite(jrubydir, fileList.toString());
        }
    }

    private DirectoryScanner scan(String[] includes, String[] excludes) {
        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(project.getBuild().getOutputDirectory());
        scanner.addDefaultExcludes();
        scanner.setIncludes(includes);
        scanner.setExcludes(excludes);
        scanner.scan();
        return scanner;
    }
}
