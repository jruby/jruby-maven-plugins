/**
 * 
 */
package de.saumya.mojo.ruby.gems;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.sonatype.aether.RepositorySystemSession;

import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;
import de.saumya.mojo.ruby.script.ScriptFactory;

public class GemsInstaller {

    private static final String OPENSSL_VERSION = "0.7.6.1";
    private static final String OPENSSL = "jruby-openssl";//-maven";
    
    private static final FileFilter FILTER = new FileFilter() {
        
        public boolean accept(File f) {
            return f.getName().endsWith(".gemspec");
        }
    };
    
    public final GemsConfig     config;

    public final ScriptFactory  factory;

    private final GemManager    manager;

    public GemsInstaller(final GemsConfig config, final ScriptFactory factory,
            final GemManager manager) {
        this.config = config;
        this.factory = factory;
        this.manager = manager;
    }

    public void installPom(final MavenProject pom) throws IOException,
            ScriptException, GemException {
        installGems(pom, null);
    }

    public void installPom(final MavenProject pom,
            final ArtifactRepository localRepository) throws IOException,
            ScriptException, GemException {
        installGems(pom, localRepository);
    }
    
    public MavenProject installOpenSSLGem(final RepositorySystemSession repositorySystemSession,
            final ArtifactRepository localRepository) throws GemException,
            IOException, ScriptException {
        return installGem(OPENSSL, OPENSSL_VERSION, repositorySystemSession, localRepository);
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
            remoteRepositories = new ArrayList<ArtifactRepository>();
            this.manager.addDefaultGemRepositories(remoteRepositories);
            artifact = this.manager.createGemArtifact(name, version);
        }
        final MavenProject pom = this.manager.buildPom(artifact,
                                                       repositorySystemSession,
                                                       localRepository,
                                                       remoteRepositories);
        installPom(pom);
        return pom;
    }

    public void installGems(final MavenProject pom, final ArtifactRepository localRepository) 
        throws IOException, ScriptException, GemException {
        installGems(pom, (Collection<Artifact>)null, localRepository);
    }
    
    public void installGems(final MavenProject pom, PluginDescriptor plugin,
            final ArtifactRepository localRepository) throws IOException,
            ScriptException, GemException {
        installGems(pom, plugin.getArtifacts(), localRepository);
    }

    public void installGems(final MavenProject pom, final Collection<Artifact> artifacts,
            final ArtifactRepository localRepository) throws IOException,
            ScriptException, GemException {
        installGems(pom, artifacts, localRepository, pom.getRemoteArtifactRepositories());
    }
    
    public void installGems(final MavenProject pom, final Collection<Artifact> artifacts,
                final ArtifactRepository localRepository, List<ArtifactRepository> remoteRepos) throws IOException,
                ScriptException, GemException {
        // start with empty script. 
        // script will be create when first un-installed gem is found
        Script script = null;
        if (pom != null) {
            boolean hasAlreadyOpenSSL = false;
            for (final Artifact artifact : pom.getArtifacts()) {
                // assume pom.getBasedir() != null indicates the project pom
                if ("compile".equals(artifact.getScope()) || "runtime".equals(artifact.getScope()) || pom.getBasedir() != null) {
                    if (!artifact.getFile().exists()) {
                        this.manager.resolve(artifact,
                                             localRepository,
                                             remoteRepos);

                    }
                    script = maybeAddArtifact(script, artifact);
                    hasAlreadyOpenSSL = hasAlreadyOpenSSL
                            || artifact.getArtifactId().equals(OPENSSL);
                }
            }
            if (artifacts != null) {
                for (final Artifact artifact : artifacts) {
                    if (!artifact.getFile().exists()) {
                        this.manager.resolve(artifact,
                                             localRepository,
                                             remoteRepos);

                    }
                    script = maybeAddArtifact(script, artifact);
                    hasAlreadyOpenSSL = hasAlreadyOpenSSL
                            || artifact.getArtifactId().equals(OPENSSL);
                }
            }
            if (pom.getArtifact().getFile() != null
            // to filter out target/classes
                    && pom.getArtifact().getFile().isFile()) {
                script = maybeAddArtifact(script, pom.getArtifact());
            }
            if (!this.config.skipJRubyOpenSSL() && !hasAlreadyOpenSSL && script != null) {
                // keep the version hard-coded to stay reproducible
                final Artifact openssl = this.manager.createGemArtifact(OPENSSL,
                                                                        OPENSSL_VERSION);

                if (pom.getFile() == null) {
                    // we do not have a pom so we need the default gems repo
                    this.manager.addDefaultGemRepositories(remoteRepos);
                }
                for(Artifact a : this.manager.resolve(openssl,
                                                      localRepository,
                                                      remoteRepos, true) ) {
                    script = maybeAddArtifact(script, a);
                }
            }
        }

        if (script != null) {
            script.addArg("--bindir", this.config.getBinDirectory());
            if(!this.config.getBinDirectory().exists()){
                this.config.getBinDirectory().mkdirs();
            }
            script.execute();
            
            // workaround for unpatched: https://github.com/rubygems/rubygems/commit/21cccd55b823848c5e941093a615b0fdd6cd8bc7
            for(File spec : new File(this.config.getGemHome(), "specifications").listFiles(FILTER)){
                String content = FileUtils.fileRead(spec);
                FileUtils.fileWrite(spec.getAbsolutePath(), content.replaceFirst(" 00:00:00.000000000Z", ""));
            }
        }
    }

    private boolean exists(Artifact artifact) {
        String basename = artifact.getArtifactId() + "-"
                + artifact.getVersion();
        String javaBasename = basename + "-java";

        for (File dir : this.config.getGemsDirectory()) {
            if (new File(dir, basename).exists()
                    || new File(dir, javaBasename).exists()) {
                return true;
            }
        }
        return false;
    }
    
    private Script maybeAddArtifact(Script script, final Artifact artifact)
            throws IOException, GemException {
        if (artifact.getType().contains("gem")) {
            if (!exists(artifact)) {
                if (script == null) {
                    script = this.factory.newScriptFromJRubyJar("gem")
                            .addArg("install")
                            .addArg("--ignore-dependencies")
                            .addArg(booleanArg(this.config.isAddRdoc(), "rdoc"))
                            .addArg(booleanArg(this.config.isAddRI(), "ri"))
                            .addArg(booleanArg(this.config.isUserInstall(),
                                               "user-install"))
                            .addArg(booleanArg(this.config.isVerbose(),
                                               "verbose"));
                }
                script.addArg(artifact.getFile());
            }
        }
        return script;
    }

    private String booleanArg(final boolean flag, final String name) {
        return "--" + (flag ? "" : "no-") + name;
    }
}