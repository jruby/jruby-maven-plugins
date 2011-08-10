package de.saumya.mojo.gems;

import java.io.IOException;

import junit.framework.TestCase;

import org.codehaus.plexus.util.IOUtil;

public class Maven2GemVersionConverterTest extends TestCase
{
	private static final String SEP = System.getProperty("line.separator");
	
    private Maven2GemVersionConverter converter;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        converter = new Maven2GemVersionConverter();
    }

    public void testSimple()
    {
        check( "1", "1.0.0", false );
        check( "1.2", "1.2.0", false );
        check( "1.2.3", "1.2.3", true );
        check( "1.2-SNAPSHOT", "1.2.0.snapshot", false );
        check( "1.2.3-SNAPSHOT", "1.2.3.snapshot", false );
        check( "1.2-3", "1.2.0.3", false );
        check( "1_2_3", "1.0.0.2.3", false );
        check( "1-2-3", "1.0.0.2.3", false );
        check( "1-2.3", "1.0.0.2.3", false );
        check( "1.2.3a", "1.2.3.a", false );
        check( "1.2.3alpha", "1.2.3.a", false );
        check( "1.2.3beta", "1.2.3.b", false );
        check( "1.2.3.gamma", "1.2.3.g", false );
        check( "2.3.3-RC1", "2.3.3.r.1", false );
        check( "1.2.3-alpha-2", "1.2.3.a.2", false );
        check( "12.23beta23", "12.23.b.23", false );
        check( "3.0-alpha-1.20020912.045138", "3.0.0.a.1.20020912.045138", false );
        check( "2.2-b1", "2.2.0.b.1", false );
        check( "2.2b1", "2.2.b.1", false );
        check( "3.3.2.GA", "3.3.2.ga", false );
        check( "3.3.0.SP1", "3.3.0.s.1", false );
        check( "3.3.0.CR1", "3.3.0.r.1", false );
        check( "1.0.0.RC3_JONAS", "1.0.0.r.3.jonas", false );
        check( "1.1.0-M1b-JONAS", "1.1.0.m.1.b.jonas", false );
        check( "2.0-m5", "2.0.0.m.5", false );
        check( "2.1_3", "2.1.0.3", false );
        check( "1.2beta4", "1.2.b.4", false );
        check( "R8pre2", "0.0.1.r.8.pre.2", false );
        check( "R8RC2.3", "0.0.1.r.8.r.2.3", false );
        check( "Somethin", "0.0.1.somethin", false );
    }

    public void testMore()
        throws IOException
    {
        String[] versions =
            IOUtil.toString( Thread.currentThread().getContextClassLoader().getResourceAsStream( "versions.txt" ) )
                .split( SEP );
        for ( String version : versions )
        {
            String gemVersion = converter.createGemVersion( version );
            assertTrue( Maven2GemVersionConverter.gemVersionPattern.matcher( gemVersion ).matches() );
            assertNotSame( Maven2GemVersionConverter.DUMMY_VERSION, gemVersion );
        }
    }

    // ==

    protected void check( String mavenVersion, String expectedVersion, boolean inputIsProperGemVersion )
    {
        String gemVersion = converter.createGemVersion( mavenVersion );

        if ( expectedVersion != null )
        {
            assertEquals( "Expected and got versions differ!", expectedVersion, gemVersion );
        }
        if ( inputIsProperGemVersion )
        {
            assertTrue( "The input is proper Gem version, SAME INSTANCE of String should be returned!",
                mavenVersion == gemVersion );
        }
        else
        {
            assertFalse( "The input is not a proper Gem version, NEW INSTANCE of String should be returned!",
                mavenVersion == gemVersion );
        }

        assertTrue( "The version \"" + gemVersion + "\" is not a proper Gem version!", isProperGemVersion( gemVersion ) );
    }

    protected boolean isProperGemVersion( String gemVersion )
    {
        return Maven2GemVersionConverter.gemVersionPattern.matcher( gemVersion ).matches();
    }

}
