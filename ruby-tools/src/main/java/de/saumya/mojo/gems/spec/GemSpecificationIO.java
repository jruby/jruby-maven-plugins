package de.saumya.mojo.gems.spec;

import java.io.IOException;

public interface GemSpecificationIO
{
    GemSpecification read( String string )
        throws IOException;

    String write( GemSpecification gemspec )
        throws IOException;
}
