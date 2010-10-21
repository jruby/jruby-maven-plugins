/**
 * 
 */
package de.saumya.mojo.ruby.gems;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystemSession;

import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.ruby.script.ScriptFactory;

public class GemsInstaller {

    private static final String JRUBY_OPENSSL = "jruby-openssl";

    public final GemsConfig     config;

    public final ScriptFactory  factory;

    private final GemManager    manager;

    public GemsInstaller(final GemsConfig config, final ScriptFactory factory,
            final GemManager manager) {
        this.config = config;
        this.factory = factory;
        this.manager = manager;
    }

    public static final String GEM_RUBY_COMMAND = "META-INF/jruby.home/bin/gem";

    public void installPom(final MavenProject pom) throws IOException,
            ScriptException, GemException {
        installGems(pom, null, null);
    }

    public void installPom(final MavenProject pom,
            final ArtifactRepository localRepository) throws IOException,
            ScriptException, GemException {
        installGems(pom, null, localRepository);
    }

    public MavenProject installGem(final String name, final String version,
            final RepositorySystemSession repositorySystemSession,
            final ArtifactRepository localRepository) throws GemException,
            IOException, ScriptException {
        final Artifact artifact;
        final List<ArtifactRepository> remoteRepositories;
        if (version == null) {
            remoteRepositories = Collections.singletonList(this.manager.defaultGemArtifactRepository());
            artifact = this.manager.createGemArtifactWithLatestVersion(name,
                                                                       localRepository,
                                                                       remoteRepositories);
        }
        else {
            remoteRepositories = Collections.singletonList(this.manager.defaultGemArtifactRepositoryForVersion(version));
            artifact = this.manager.createGemArtifact(name, version);
        }
        final MavenProject pom = this.manager.buildPom(artifact,
                                                       repositorySystemSession,
                                                       localRepository,
                                                       remoteRepositories);
        installPom(pom);
        return pom;
    }

    public void installGems(final MavenProject pom, final Artifact ensureGem,
            final ArtifactRepository localRepository) throws IOException,
            ScriptException, GemException {
        // start with empty script which will be create when first
        // un-installed gem is found

        Script script = null;
        if (ensureGem != null) {
            if (ensureGem.getFile() == null) {
                this.manager.resolve(ensureGem,
                                     localRepository,
                                     Collections.singletonList(this.manager.defaultGemArtifactRepositoryForVersion(ensureGem.getVersion())));
            }
            script = maybeAddArtifact(script, ensureGem);
        }

        if (pom != null) {
            boolean hasAlreadyOpenSSL = false;
            for (final Artifact artifact : pom.getArtifacts()) {
                if (!artifact.getFile().exists()) {
                    this.manager.resolve(artifact,
                                         localRepository,
                                         pom.getRemoteArtifactRepositories());

                }
                script = maybeAddArtifact(script, artifact);
                hasAlreadyOpenSSL = hasAlreadyOpenSSL
                        || artifact.getArtifactId().equals(JRUBY_OPENSSL);
            }
            if (pom.getArtifact().getFile() != null
            // to filter out target/classes
                    && pom.getArtifact().getFile().isFile()) {
                script = maybeAddArtifact(script, pom.getArtifact());
            }
            if (this.config.skipJRubyOpenSSL() && !hasAlreadyOpenSSL) {
                final Artifact openssl = this.manager.createGemArtifact(JRUBY_OPENSSL,
                                                                        "0.7");

                this.manager.resolve(openssl,
                                     localRepository,
                                     pom.getRemoteArtifactRepositories());
                script = maybeAddArtifact(script, openssl);
            }
        }

        if (script != null) {
            script.addArg("--bindir", this.config.getBinDirectory());
            script.execute();
        }
    }

    private Script maybeAddArtifact(Script script, final Artifact artifact)
            throws IOException, GemException {
        final File gemDir = new File(this.config.getGemsDirectory(),
                artifact.getArtifactId() + "-" + artifact.getVersion());
        final File javaGemDir = new File(gemDir.getPath() + "-java");
        if (artifact.getType().contains("gem")
                && !(gemDir.exists() || javaGemDir.exists())) {
            if (script == null) {
                script = this.factory.newScriptFromResource(GEM_RUBY_COMMAND)
                        .addArg("install")
                        .addArg("--ignore-dependencies")
                        .addArg(booleanArg(this.config.isAddRdoc(), "rdoc"))
                        .addArg(booleanArg(this.config.isAddRI(), "ri"))
                        .addArg(booleanArg(this.config.isUserInstall(),
                                           "user-install"))
                        .addArg(booleanArg(this.config.isVerbose(), "verbose"));
            }
            script.addArg(artifact.getFile());
        }
        return script;
    }

    private String booleanArg(final boolean flag, final String name) {
        return "--" + (flag ? "" : "no-") + name;
    }
}