/**
 * 
 */
package de.saumya.mojo.gems;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.saumya.mojo.ruby.Logger;
import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.ruby.script.ScriptFactory;

public class GemspecConverter {

    private final ScriptFactory factory;
    private final Logger        log;

    public GemspecConverter(final Logger log, final ScriptFactory factory) {
        this.factory = factory;
        this.log = log;
    }

    public void createPom(final File gemspec,
            final String jrubyMavenPluginsVersion, final File pom)
            throws ScriptException, IOException {
        this.factory.newScriptFromResource("gem2pom.rb")
                .addArg(gemspec.getAbsolutePath())
                .addArg(jrubyMavenPluginsVersion)
                .execute(pom);
    }

    /*
     * this method is very problematic since you end up with metadata (version
     * list of an groupId-artifactId pair) on your local repository where you
     * might not be able to download pom/gem for the calculated version. so it
     * would only make sense if the all the poms are generated as well and the
     * gem download just passes the request on to rubygems.org. BUT the latter
     * is difficult since there is not a proper "file not found" coming from the
     * rubygems server. . . .
     */
    public void updateMetadata(final List<String> remoteRepositoryIds,
            final String localRepositoryBasedir) throws ScriptException,
            IOException {
        for (final String id : remoteRepositoryIds) {
            if (id.startsWith("rubygems")) {
                this.log.info("updating metadata for " + id);
                this.factory.newScriptFromResource("update_metadata.rb")
                        .addArg(id)
                        .addArg(localRepositoryBasedir)
                        .execute();
            }
        }
    }
}