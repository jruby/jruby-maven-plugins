/**
 * 
 */
package de.saumya.mojo;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GemService {

    private final Launcher launcher;
    private final Log      log;

    public GemService(final Log log, final Launcher launcher) {
        this.launcher = launcher;
        this.log = log;
    }

    public void convertGemspec2Pom(final File gemspec, final File pom)
            throws RubyScriptException, IOException {
        this.launcher.executeScript("gem2pom.rb",
                                    pom,
                                    gemspec.getAbsolutePath(),
				    // TODO pass this into the service
				    "0.20.0"); // plugin version
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
            final String localRepositoryBasedir) throws RubyScriptException,
            IOException {
        for (final String id : remoteRepositoryIds) {
            if (id.startsWith("rubygems")) {
                this.log.info("updating metadata for " + id);
                this.launcher.executeScript("update_metadata.rb",
                                            id,
                                            localRepositoryBasedir);
            }
        }
    }
}