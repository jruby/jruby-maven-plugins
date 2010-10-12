/**
 * 
 */
package de.saumya.mojo.proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.jruby.embed.ScriptingContainer;

import de.saumya.mojo.ruby.GemScriptingContainer;

class ControllerService {

    private final Object             rubyObject;

    private final ScriptingContainer scriptingContainer;

    private long                     blockUpdatesUntil = 0;

    private final Object             mutex             = new Object();

    ControllerService(final GemScriptingContainer scripting) throws IOException {
        this.rubyObject = scripting.runScriptletFromClassloader("gem_artifacts.rb");
        this.scriptingContainer = scripting;
        update();
    }

    boolean update() {
        boolean update = false;
        synchronized (this.mutex) {
            if (this.blockUpdatesUntil < System.currentTimeMillis()) {
                // next update 12 hours later
                this.blockUpdatesUntil = System.currentTimeMillis() + 12 * 60
                        * 60 * 1000;
                update = true;
            }
        }
        if (update) {
            this.scriptingContainer.callMethod(this.rubyObject,
                                               "update",
                                               Object.class);
        }
        return update;
    }

    String getGemLocation(final String name, final String version)
            throws FileNotFoundException {
        final String uri = this.scriptingContainer.callMethod(this.rubyObject,
                                                              "gem_location",
                                                              new String[] {
                                                                      name,
                                                                      version },
                                                              String.class);
        if (uri == null) {
            throw new FileNotFoundException();
        }
        return uri;
    }

    void spec2Pom(final File specFile, final File pomFile) {
        this.scriptingContainer.callMethod(this.rubyObject,
                                           "spec_to_pom",
                                           new String[] {
                                                   specFile.getAbsolutePath(),
                                                   pomFile.getAbsolutePath() },
                                           Object.class);
    }

    void writePom(final String name, final String version, final Writer writer)
            throws IOException {
        final String file = this.scriptingContainer.callMethod(this.rubyObject,
                                                               "to_pom",
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

    void writeMetaData(final String name, final Writer writer,
            final boolean prereleases) throws IOException {
        final String file = this.scriptingContainer.callMethod(this.rubyObject,
                                                               "metadata",
                                                               new Object[] {
                                                                       name,
                                                                       prereleases },
                                                               String.class);
        writeout(writer, file);
    }

    void writeMetaDataSHA1(final String name, final Writer writer,
            final boolean prereleases) throws IOException {
        final String file = this.scriptingContainer.callMethod(this.rubyObject,
                                                               "metadata_sha1_file",
                                                               new Object[] {
                                                                       name,
                                                                       prereleases },
                                                               String.class);
        writeout(writer, file);
    }

    private void writeout(final Writer writer, final String file)
            throws FileNotFoundException, IOException {
        if (file == null) {
            throw new FileNotFoundException();
        }
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