package de.saumya.mojo.gems;

import java.io.File;
import java.io.IOException;

import de.saumya.mojo.gems.spec.GemSpecification;

/**
 * This is the single entry point into the Maven artifact to Ruby Gem converter.
 * 
 * @author cstamas
 */
public interface MavenArtifactConverter {

    public static final String GEMNAME_PREFIX = "mvn:";

    /**
     * Returns is the artifact convertable safely into Gem.
     * 
     * @param artifact
     * @return true if yes.
     */
    boolean canConvert(MavenArtifact artifact);

    /**
     * Returns the "regular" gem filename, as it is expected this artifact to be
     * called as Gem.
     * 
     * @param artifact
     * @return
     */
    String getGemFileName(MavenArtifact artifact);

    /**
     * Creates a Gem::Specification (the equivalent JavaBeans actually) filled
     * up properly based on informaton from POM. The "better" POM is, the getter
     * is gemspec. For best results, fed in interpolated POMs!
     * 
     * @param artifact
     * @return
     */
    GemSpecification createSpecification(MavenArtifact artifact);

    File createGemspecFromArtifact(MavenArtifact artifact, File target)
            throws IOException;

    /**
     * Creates a valid Ruby Gem, and returns File pointing to the result.
     * 
     * @param artifact
     *            the artifact to gemize (without data only gemspec)
     * @param target
     *            where to save Gem file. If null, it will be created next to
     *            artifact
     * @return
     * @throws IOException
     */
    GemArtifact createGemStubFromArtifact(MavenArtifact artifact, File target)
            throws IOException;

    /**
     * Creates a valid Ruby Gem, and returns File pointing to the result.
     * 
     * @param artifact
     *            the artifact to gemize
     * @param target
     *            where to save Gem file. If null, it will be created next to
     *            artifact
     * @return
     * @throws IOException
     */
    GemArtifact createGemFromArtifact(MavenArtifact artifact, File target)
            throws IOException;
}
