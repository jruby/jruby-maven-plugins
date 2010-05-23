/**
 * 
 */
package de.saumya.mojo.proxy;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.jruby.embed.ScriptingContainer;

class ControllerService {

    private final Object             rubyObject;

    private final ScriptingContainer scriptingContainer;

    ControllerService(final JRubyService jruby) throws IOException {
        this.rubyObject = jruby.rubyObject("gem_artifacts.rb");
        this.scriptingContainer = jruby.scripting();
        update();
    }

    void update() {
        this.scriptingContainer.callMethod(this.rubyObject,
                                           "update",
                                           Object.class);
    }

    String getGemLocation(final String name, final String version) {
        return "http://rubygems.org/gems/" + name + "-" + version + ".gem";
    }

    void writePom(final String name, final String version, final Writer writer)
            throws IOException {
        final String file = this.scriptingContainer.callMethod(this.rubyObject,
                                                               "spec_to_pom",
                                                               new String[] {
                                                                       name,
                                                                       version },
                                                               String.class);
        writeout(writer, file);
    }

    void writePomSHA1(final String name, final String version,
            final Writer writer) throws IOException {
        final String file = this.scriptingContainer.callMethod(this.rubyObject,
                                                               "pom_sha1_file",
                                                               new String[] {
                                                                       name,
                                                                       version },
                                                               String.class);
        writeout(writer, file);
    }

    void writeGemSHA1(final String name, final String version,
            final Writer writer) throws IOException {
        final String file = this.scriptingContainer.callMethod(this.rubyObject,
                                                               "gem_sha1_file",
                                                               new String[] {
                                                                       name,
                                                                       version },
                                                               String.class);
        writeout(writer, file);
    }

    void writeMetaData(final String name, final Writer writer)
            throws IOException {
        final String file = this.scriptingContainer.callMethod(this.rubyObject,
                                                               "metadata",
                                                               new String[] { name },
                                                               String.class);
        writeout(writer, file);
    }

    void writeMetaDataSHA1(final String name, final Writer writer)
            throws IOException {
        final String file = this.scriptingContainer.callMethod(this.rubyObject,
                                                               "metadata_sha1_file",
                                                               new String[] { name },
                                                               String.class);
        writeout(writer, file);
    }

    private void writeout(final Writer writer, final String file)
            throws FileNotFoundException, IOException {
        // TODO make sure utf-8 is used
        final Reader in = new BufferedReader(new FileReader(file));
        try {
            int c = in.read();
            while (c != -1) {
                writer.write(c);
                c = in.read();
            }
        }
        finally {
            in.close();
        }
    }
}