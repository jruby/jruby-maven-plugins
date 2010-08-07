package de.saumya.mojo.gems;

/**
 * This class does NOT represent full Maven2 coordinates. This is only about "primary" artifacts, that does not have
 * classifiers.
 * 
 * @author cstamas
 */
public class ArtifactCoordinates
{
    private String groupId;

    private String artifactId;

    private String version;

    private String extension;

    public ArtifactCoordinates( String groupId, String artifactId, String version )
    {
        this( groupId, artifactId, version, "jar" );
    }

    public ArtifactCoordinates( String groupId, String artifactId, String version, String extension )
    {
        this.groupId = groupId;

        this.artifactId = artifactId;

        this.version = version;

        this.extension = extension;
    }

    protected String getGroupId()
    {
        return groupId;
    }

    protected void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    protected String getArtifactId()
    {
        return artifactId;
    }

    protected void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    protected String getVersion()
    {
        return version;
    }

    protected void setVersion( String version )
    {
        this.version = version;
    }

    protected String getExtension()
    {
        return extension;
    }

    protected void setExtension( String extension )
    {
        this.extension = extension;
    }

    // ==

    public String toString()
    {
        return getGroupId() + ":" + getArtifactId() + ":" + getVersion() + ":(" + getExtension() + ")";
    }

}
