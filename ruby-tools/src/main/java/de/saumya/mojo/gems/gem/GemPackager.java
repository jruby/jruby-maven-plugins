package de.saumya.mojo.gems.gem;

import java.io.File;
import java.io.IOException;

import de.saumya.mojo.gems.spec.GemSpecification;

/**
 * A low level component that manufactures the actual Gem file.
 * 
 * @author cstamas
 * @author mkristian
 */
public interface GemPackager {
    /**
     * This method will create the GEM stub with only gemspec and not data. It
     * will do NO validation at all, just blindly create the Gem using supplied
     * stuff.
     * 
     * @param gemspec
     *            The Gem::Specification to embed into Gem.
     * @param targetDirectory
     *            The directory where the manufactured Gem should be saved.
     * @return gemFile The File location of the manufactured Gem.
     * @throws IOException
     */
    File createGemStub(GemSpecification gemspec, File targetDirectory)
            throws IOException;

    /**
     * This method will create the GEM. It will do NO validation at all, just
     * blindly create the Gem using supplied stuff.
     * 
     * @param gem
     *            The Gem::Specification and the files to embed into Gem.
     * @param targetDirectory
     *            The directory where the manufactured Gem should be saved.
     * @return gemFile The File location of the manufactured Gem.
     * @throws IOException
     */
    File createGem(Gem gem, File targetDirectory) throws IOException;
}
