package de.saumya.mojo.gems;

import java.io.File;

import org.apache.maven.model.Model;

/**
 * This bean holds the artifact to be converted. Model should be already loaded up, to support different loading
 * strategies (ie. from pom.xml, from JAR itself, or using something like Maven2 support in Nexus or having real
 * interpolated POM).
 */
public class MavenArtifact
{
    private final Model pom;

    private final ArtifactCoordinates coordinates;

    private final File artifactFile;

    public MavenArtifact( Model pom, ArtifactCoordinates coordinates, File artifact )
    {
        this.pom = pom;

        this.coordinates = coordinates;

        this.artifactFile = artifact;
    }

    public Model getPom()
    {
        return pom;
    }

    protected ArtifactCoordinates getCoordinates()
    {
        return coordinates;
    }

    public File getArtifactFile()
    {
        return artifactFile;
    }
}
