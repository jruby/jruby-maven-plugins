package de.saumya.mojo.jruby9.jar;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * TODO
 */
@Mojo( name = "process", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresProject = true,
       threadSafe = true )
public class ProcessMojo extends AbstractMojo {

    static class JRubyDirectory extends SimpleFileVisitor<Path> {
        
        Map<Path, List<String>> dirs = new HashMap<Path,List<String>>();
        private Path root;

        JRubyDirectory(Path root) {
            this.root = root;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir,
                BasicFileAttributes attrs) throws IOException {
            String name = dir.getParent().toFile().getName();
            if ("jars".equals(name) || "META-INF".equals(name)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            add(dir);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
            String name = file.toFile().getName();
            if(name.endsWith(".class") || name.equals(".jrubydir") || (file.getParent().equals(root) && 
                    ("gems".equals(name) || "specifications".equals(name) ||
                     "jars".equals(name) || "jar-bootstrap.rb".equals(name)))) {
                return FileVisitResult.CONTINUE;                
            }
            add(file);
            return FileVisitResult.CONTINUE;
        }

        private void add(Path file) {
            String name = file.toFile().getName();
            List<String> dir = dirs.get(file.getParent());
            if (dir == null) {
                dir =  new LinkedList<String>();
                dirs.put(file.getParent(), dir);
            }
            dir.add(name);
        }
        
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
            File jrubyDir = dir.resolve(".jrubydir").toFile();
            List<String> names = dirs.remove(dir);
            if (names != null) {
                StringBuilder content = new StringBuilder(".\n");
                if (!dir.equals(root)) content.append("..\n");
                for(String name: names) {
                    content.append(name).append("\n");
                }
                if (jrubyDir.exists()) {
                    String old = FileUtils.readFileToString(jrubyDir);
                    if (content.equals(old)) {
                        return FileVisitResult.CONTINUE;
                    }
                }
                FileUtils.write(jrubyDir, content);
            }
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * The Maven project.
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        Path root = new File(project.getBuild().getOutputDirectory()).toPath();
        try {
            Files.walkFileTree(root, new JRubyDirectory(root));
        } catch (IOException e) {
            throw new MojoExecutionException("could not generate .jrubydir files", e);
        }
    }

}
