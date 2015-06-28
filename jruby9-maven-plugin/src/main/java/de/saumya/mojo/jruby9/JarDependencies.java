package de.saumya.mojo.jruby9;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.artifact.Artifact;

public class JarDependencies {
    private final List<Artifact> artifacts = new LinkedList<Artifact>();

    void add(Artifact a) {
        if (a.getType().equals("jar")) {
            artifacts.add(a);
        }
    }
    
    void generateJarsLock(File file) throws IOException {
        try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            for(Artifact a: artifacts) {
                out.print(a);
                out.println(":");
            }
        }
    }
}