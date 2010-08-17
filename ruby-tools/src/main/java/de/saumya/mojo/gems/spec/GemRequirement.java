package de.saumya.mojo.gems.spec;

import java.util.ArrayList;
import java.util.List;

/**
 * Gem::Requirement
 * 
 * @author cstamas
 */
public class GemRequirement
{
    private List<Object> requirements;

    private String version;

    public List<Object> getRequirements()
    {
        if ( requirements == null )
        {
            requirements = new ArrayList<Object>();
        }

        return requirements;
    }

    public void setRequirements( List<Object> requirements )
    {
        this.requirements = requirements;
    }

    public void addRequirement( String relation, GemVersion version )
    {
        ArrayList<Object> tupple = new ArrayList<Object>( 2 );

        tupple.add( relation );

        tupple.add( version );

        getRequirements().add( tupple );
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }
}
