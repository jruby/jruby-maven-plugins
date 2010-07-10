/**
 * 
 */
package de.saumya.mojo;

import java.io.File;
import java.io.IOException;

public class GemspecService {

    private final Launcher launcher;
    private final File     localRepository;

    public GemspecService(final Launcher launcher, final File localRepository) {
        this.launcher = launcher;
        this.localRepository = localRepository;
    }

    public void convertGemspec2Pom(final File gemspec, final File pom)
            throws RubyScriptException, IOException {
        this.launcher.executeScript("gem2pom.rb",
                                    pom,
                                    gemspec.getAbsolutePath(),
                                    this.localRepository.getAbsolutePath());
    }
}