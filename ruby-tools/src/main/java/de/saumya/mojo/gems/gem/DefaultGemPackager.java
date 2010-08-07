package de.saumya.mojo.gems.gem;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.gzip.GZipArchiver;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarArchiver.TarCompressionMethod;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogger;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;

import de.saumya.mojo.gems.spec.GemSpecification;
import de.saumya.mojo.gems.spec.GemSpecificationIO;

@Component(role = GemPackager.class)
public class DefaultGemPackager implements GemPackager {

    @Requirement(hints = { "yaml" })
    private GemSpecificationIO gemSpecificationIO;

    public File createGemStub(final GemSpecification gemspec, final File target)
            throws IOException {
        return createGem(new Gem(gemspec), target);
    }

    public File createGem(final Gem gem, final File target) throws IOException {
        final File gemWorkdir = new File(File.createTempFile("nexus-gem-work",
                                                             ".tmp")
                .getParentFile(), "wd-" + System.currentTimeMillis());

        gemWorkdir.mkdirs();

        if (!gem.getGemFiles().isEmpty()) {
            for (final GemFileEntry entry : gem.getGemFiles()) {
                if (!entry.getSource().isFile()) {
                    throw new IOException("The GEM entry must be a file!");
                }
            }
        }

        // get YAML
        final String gemspecString = this.gemSpecificationIO.write(gem.getSpecification());

        // DEBUG
        // FileUtils.fileWrite( gemFile.getAbsolutePath(), gemspecString );
        // DEBUG

        try {
            // write file "metadata" (YAML of gemspec)
            final File metadata = new File(gemWorkdir, "metadata");
            final File metadataGz = new File(gemWorkdir, "metadata.gz");
            FileUtils.fileWrite(metadata.getAbsolutePath(),
                                "UTF-8",
                                gemspecString);
            // gzip it into metadata.gz
            final GZipArchiver gzip = new GZipArchiver();
            gzip.setDestFile(metadataGz);
            gzip.addFile(metadata, "metadata.gz");
            gzip.createArchive();

            final TarArchiver tar = new TarArchiver();
            // turn off logging
            tar.enableLogging(new AbstractLogger(0, "nologging") {

                public void warn(final String message, final Throwable throwable) {
                }

                public void info(final String message, final Throwable throwable) {
                }

                public Logger getChildLogger(final String name) {
                    return null;
                }

                public void fatalError(final String message,
                        final Throwable throwable) {
                }

                public void error(final String message,
                        final Throwable throwable) {
                }

                public void debug(final String message,
                        final Throwable throwable) {
                }
            });
            final TarCompressionMethod compression = new TarCompressionMethod();
            File dataTarGz = null;
            if (!gem.getGemFiles().isEmpty()) {
                // tar.gz the content into data.tar.gz
                dataTarGz = new File(gemWorkdir, "data.tar.gz");
                compression.setValue("gzip");
                tar.setCompression(compression);
                tar.setDestFile(dataTarGz);
                for (final GemFileEntry entry : gem.getGemFiles()) {
                    if (entry.getSource().isFile()) {
                        tar.addFile(entry.getSource(), entry.getPathInGem());
                    }
                    else if (entry.getSource().isDirectory()) {
                        tar.addDirectory(entry.getSource(),
                                         entry.getPathInGem());
                    }
                }
                tar.createArchive();
            }

            // and finally create gem by tar.gz-ing data.tar.gz and metadata.gz
            final File gemFile = new File(target, gem.getGemFilename());
            tar.setDestFile(gemFile);
            compression.setValue("none");
            tar.setCompression(compression);
            if (dataTarGz != null) {
                tar.addFile(dataTarGz, dataTarGz.getName());
            }
            tar.addFile(metadataGz, metadataGz.getName());
            tar.createArchive();

            return gemFile;
        }
        catch (final ArchiverException e) {
            final IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
        finally {
            FileUtils.forceDelete(gemWorkdir);
        }
    }
}
