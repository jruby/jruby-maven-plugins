/**
 * 
 */
package de.saumya.mojo.gem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.repository.legacy.DefaultWagonManager;
import org.apache.maven.repository.legacy.WagonManager;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jruby.embed.ScriptingContainer;

@Component(role = WagonManager.class, hint = "gem")
public class GemWagonManger extends DefaultWagonManager {

    @Requirement
    // avoid naming conflict when injecting the logger
    private Logger gemLogger;

    @Override
    public void getArtifactMetadata(final ArtifactMetadata metadata,
            final ArtifactRepository repository, final File destination,
            final String checksumPolicy) throws TransferFailedException,
            ResourceDoesNotExistException {
        System.out.println("-------------------------------- asdfasfasfsa");
        // instanceof does not work probably a classloader issue !!!
        if (repository.getLayout()
                .getClass()
                .getName()
                .equals(GemRepositoryLayout.class.getName())
                && metadata.getGroupId().equals("rubygems")) {
            try {
                createMetadata(metadata.getArtifactId(), destination);
            }
            catch (final IOException e) {
                throw new TransferFailedException("error writing metadata for "
                        + metadata, e);
            }
        }
        else {
            super.getArtifactMetadata(metadata,
                                      repository,
                                      destination,
                                      checksumPolicy);
        }
    }

    private void createMetadata(final String gemName, final File destination)
            throws IOException {
        this.gemLogger.info("creating metadata for gem " + gemName);
        System.out.println("creating metadata for gem " + gemName);
        destination.getParentFile().mkdirs();
        final ScriptingContainer container = new ScriptingContainer();
        final String script = "ARGV[0] = '" + gemName + "'\nrequire '"
                + embeddedRubyFile("metadata.rb") + "'";
        container.setWriter(new FileWriter(destination));

    }

    protected String embeddedRubyFile(final String rubyFile) {
        return Thread.currentThread()
                .getContextClassLoader()
                .getResource(rubyFile)
                .toExternalForm();
    }
}