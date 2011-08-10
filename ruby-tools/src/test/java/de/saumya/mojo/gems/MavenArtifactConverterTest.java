package de.saumya.mojo.gems;

import java.io.File;
import java.io.FileReader;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import de.saumya.mojo.gems.spec.GemSpecification;
import de.saumya.mojo.gems.spec.GemSpecificationIO;
import de.saumya.mojo.ruby.GemScriptingContainer;

/**
 * Unit test for simple App.
 */
public class MavenArtifactConverterTest extends PlexusTestCase {
    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(MavenArtifactConverterTest.class);
    }

    private GemScriptingContainer scripting;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final File gems = new File(new File(getBasedir(), "target"), "rubygems");
        this.scripting = new GemScriptingContainer(gems, gems);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSpecifiction() throws Exception {
        final File yamlFile = new File("src/test/resources/metadata-prawn");

        final String originalYamlString = FileUtils.fileRead(yamlFile);

        final GemSpecificationIO gemSpecIO = lookup(GemSpecificationIO.class);

        final GemSpecification gemSpec = gemSpecIO.read(originalYamlString);

        final String dumpedYamlString = gemSpecIO.write(gemSpec);

        System.out.println("snakeYAML ****");
        System.out.println(dumpedYamlString);

        // will fail -- snakeYaml "sorts" properties alphabetically!
        // a compare by value, and as string would maybe work
        // Assert.assertEquals( originalYamlString, dumpedYamlString );
    }

    public void testConversion() throws Exception {
        doConversion("org/slf4j/slf4j-api/1.5.8/slf4j-api-1.5.8.pom",
                     new ArtifactCoordinates("org.slf4j", "slf4j-api", "1.5.8"));
        doConversion("org/slf4j/slf4j-simple/1.5.8/slf4j-simple-1.5.8.pom",
                     new ArtifactCoordinates("org.slf4j",
                             "slf4j-simple",
                             "1.5.8"));
        doConversion("org/apache/ant/ant-parent/1.7.1/ant-parent-1.7.1.pom",
                     new ArtifactCoordinates("org.apache.ant",
                             "ant-parent",
                             "1.7.1"));

        // load helper script
        final Object gemTester = this.scripting.runScriptletFromClassloader("gem_tester.rb",
                                                                          getClass());
        // this.scriptingContainer.runScriptlet(getClass().getResourceAsStream("gem_tester.rb"),
        // "gem_tester.rb");

        // setup local rubygems repository
        final File rubygems = new File(getBasedir(), "target/rubygems");
        rubygems.mkdirs();
        this.scripting.callMethod(gemTester,
                                  "setup_gems",
                                  rubygems.getAbsolutePath(),
                                  Object.class);

        // install the slf4j gems
        this.scripting.callMethod(gemTester,
                                  "install_gems",
                                  new String[] {
                                          "target/gems/mvn.org.slf4j.slf4j-api-1.5.8-java.gem",
                                          "target/gems/mvn.org.slf4j.slf4j-simple-1.5.8-java.gem" },
                                  Object.class);
        // TODO do not know why this is needed. but without it the first run
        // fails and any successive runs succeeds !!
        this.scripting.callMethod(gemTester,
                                  "gem",
                                  "mvn.org.slf4j.slf4j-simple",
                                  Object.class);

        // load the slf4j-simple
        Boolean result = this.scripting.callMethod(gemTester,
                                                   "require_gem",
                                                   "maven/org.slf4j/slf4j-simple",
                                                   Boolean.class);
        assertTrue(result);

        // slf4j-api is already loaded as dependency of slf4j-simple
        result = this.scripting.callMethod(gemTester,
                                           "require_gem",
                                           "maven/org.slf4j/slf4j-api",
                                           Boolean.class);
        assertFalse(result);
    }

    public GemArtifact doConversion(final String pomPath,
            final ArtifactCoordinates coords) throws Exception {
        final File pomFile = new File(new File("src/test/resources/repository"),
                pomPath);

        final MavenArtifactConverter converter = lookup(MavenArtifactConverter.class);

        File artifactFile = new File(pomFile.getParentFile(), pomFile.getName()
                .replace(".pom", ".jar"));

        if (!artifactFile.isFile()) {
            artifactFile = null;
        }

        final MavenXpp3Reader reader = new MavenXpp3Reader();

        final Model pom = reader.read(new FileReader(pomFile));

        final MavenArtifact artifact = new MavenArtifact(pom,
                coords,
                artifactFile);

        final GemArtifact gemArtifact = converter.createGemFromArtifact(artifact,
                                                                        getTestFile("target/gems/"));
        assertEquals(converter.getGemFileName(artifact),
                     gemArtifact.getGemFile().getName());
        return gemArtifact;
    }
}