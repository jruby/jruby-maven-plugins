package de.saumya.mojo.gems.spec;

/**
 * Gem::Dependency
 * 
 * @author cstamas
 */
public class GemDependency
{
    private String name;

    private String type;

    private GemRequirement version_requirements;

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public GemRequirement getVersion_requirement()
    {
        return null;
    }

    public void setVersion_requirement( GemRequirement versionRequirement )
    {
        setVersion_requirements( versionRequirement );
    }

    public GemRequirement getVersion_requirements()
    {
        return version_requirements;
    }

    public void setVersion_requirements( GemRequirement versionRequirements )
    {
        version_requirements = versionRequirements;
    }
}
