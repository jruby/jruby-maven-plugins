package de.saumya.mojo.gems;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.License;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.velocity.VelocityComponent;

import de.saumya.mojo.gems.gem.Gem;
import de.saumya.mojo.gems.gem.GemPackager;
import de.saumya.mojo.gems.spec.GemDependency;
import de.saumya.mojo.gems.spec.GemRequirement;
import de.saumya.mojo.gems.spec.GemSpecification;
import de.saumya.mojo.gems.spec.GemSpecificationIO;
import de.saumya.mojo.gems.spec.GemVersion;
import de.saumya.mojo.ruby.gems.GemManager;

/**
 * This is full of "workarounds" here, since for true artifact2gem conversion I
 * would need interpolated POM!
 * 
 * @author cstamas
 * @author mkristian
 */
@Component(role = MavenArtifactConverter.class)
public class DefaultMavenArtifactConverter implements MavenArtifactConverter {
    private static final String LIB_MAVEN_PATH = "lib/maven/";

    enum RubyDependencyType {

        RUNTIME, DEVELOPMENT;

        @Override
        public String toString() {
            return ":" + name().toLowerCase();
        }

        public static RubyDependencyType toRubyDependencyType(
                final String dependencyScope) {
            // ruby scopes
            // :development
            // :runtime
            if ("provided".equals(dependencyScope)
                    || "test".equals(dependencyScope)) {
                return DEVELOPMENT;
            }
            else if ("compile".equals(dependencyScope)
                    || "runtime".equals(dependencyScope)) {
                return RUNTIME;
            }
            else
            // dependencyScope: "system"
            {
                // TODO better throw an exception since there will be no gem for
                // such a dependency or something else
                return RUNTIME;
            }

        }
    }

    /**
     * The Java platform key.
     */
    String                                  PLATFORM_JAVA             = "java";

    @Requirement
    private GemPackager                     gemPackager;

    @Requirement
    private VelocityComponent               velocityComponent;

    @Requirement(hints = { "yaml" })
    private GemSpecificationIO              gemSpecificationIO;

    private final Maven2GemVersionConverter maven2GemVersionConverter = new Maven2GemVersionConverter();

    public boolean canConvert(final MavenArtifact artifact) {
        // TODO: this is where we filter currently what to convert.
        // for now, we convert only POMs with packaging "pom", or "jar" (but we
        // ensure there is the primary artifact JAR
        // also
        // RELAXING: doing "pom" packagings but also anything that has primary
        // artifact ending with ".jar".
        if (canConvert(artifact, "pom", null)) {
            return true;
        }

        if (canConvert(artifact, artifact.getPom().getPackaging(), "jar")) {
            return true;
        }

        return false;
    }

    private boolean canConvert(final MavenArtifact artifact,
            final String packaging, final String extension) {
        String fixedExtension = null;

        if (extension != null) {
            fixedExtension = extension.startsWith(".") ? extension : "."
                    + extension;
        }

        return StringUtils.equals(packaging, artifact.getPom().getPackaging())
                && ((extension == null && artifact.getArtifactFile() == null) || (extension != null
                        && artifact.getArtifactFile() != null && artifact.getArtifactFile()
                        .getName()
                        .endsWith(fixedExtension)));
    }

    public String getGemFileName(final MavenArtifact artifact) {
        return getGemFileName(artifact.getCoordinates().getGroupId(),
                              artifact.getCoordinates().getArtifactId(),
                              artifact.getCoordinates().getVersion(),
                              this.PLATFORM_JAVA);
    }

    public GemSpecification createSpecification(final MavenArtifact artifact) {
        final GemSpecification result = new GemSpecification();

        // this is fix
        result.setPlatform(this.PLATFORM_JAVA);

        // the must ones
        result.setName(createGemName(artifact.getCoordinates().getGroupId(),
                                     artifact.getCoordinates().getArtifactId(),
                                     artifact.getCoordinates().getVersion()));
        result.setVersion(new GemVersion(createGemVersion(artifact.getCoordinates()
                .getVersion())));

        // dependencies
        if (artifact.getPom().getDependencies().size() > 0) {
            for (final Dependency dependency : artifact.getPom()
                    .getDependencies()) {
                if (!dependency.isOptional()) {
                    result.getDependencies().add(convertDependency(artifact,
                                                                   dependency));
                }
            }
        }

        // and other stuff "nice to have"
        result.setDate(new Date()); // now
        result.setDescription(sanitizeStringValue(artifact.getPom()
                .getDescription() != null
                ? artifact.getPom().getDescription()
                : artifact.getPom().getName()));
        result.setSummary(sanitizeStringValue(artifact.getPom().getName()));
        result.setHomepage(sanitizeStringValue(artifact.getPom().getUrl()));

        if (artifact.getPom().getLicenses().size() > 0) {
            for (final License license : artifact.getPom().getLicenses()) {
                result.getLicenses().add(sanitizeStringValue(license.getName()
                        + " (" + license.getUrl() + ")"));
            }
        }
        if (artifact.getPom().getDevelopers().size() > 0) {
            for (final Developer developer : artifact.getPom().getDevelopers()) {
                result.getAuthors().add(sanitizeStringValue(developer.getName()
                        + " (" + developer.getEmail() + ")"));
            }
        }

        // by default, we pack into lib/ inside gem (where is the jar and the
        // stub ruby)
        result.getRequire_paths().add("lib");
        return result;
    }

    public GemArtifact createGemStubFromArtifact(final MavenArtifact artifact,
            final File target) throws IOException {
        final GemSpecification gemspec = createSpecification(artifact);

        if (target == null) {
            throw new IOException("Must specify target file, where to generate Gem!");
        }

        // write file
        final File gemfile = this.gemPackager.createGemStub(gemspec, target);

        return new GemArtifact(gemspec, gemfile);
    }

    public GemArtifact createGemFromArtifact(final MavenArtifact artifact,
            final File target) throws IOException {
        final GemSpecification gemspec = createSpecification(artifact);

        if (target == null || (target.exists() && !target.isDirectory())) {
            throw new IOException("Must specify target directory, where to generate Gem!");
        }

        final Gem gem = new Gem(gemspec);

        if (artifact.getArtifactFile() != null) {
            gem.addFile(artifact.getArtifactFile(), createLibFileName(artifact,
                                                                      ".jar"));
        }

        // create "meta" ruby file
        final String rubyStubMetaPath = createLibFileName(artifact,
                                                          "-maven-meta.rb");
        // System.err.println( rubyStubMetaPath );
        final File rubyStubMetaFile = generateRubyMetaStub(gemspec, artifact);
        gem.addFile(rubyStubMetaFile, rubyStubMetaPath);

        // create runtime ruby file
        final String rubyStubPath = createLibFileName(artifact, ".rb");

        // System.err.println( rubyStubPath );
        final File rubyStubFile = generateRubyStub(gemspec,
                                                   artifact,
                                                   RubyDependencyType.RUNTIME);
        gem.addFile(rubyStubFile, rubyStubPath);

        // create development ruby file
        final String rubyDevelopmentStubPath = createLibFileName(artifact,
                                                                 "-dev.rb");
        // System.err.println( rubyDevelopmentStubPath );
        final File rubyDevelopmentStubFile = generateRubyStub(gemspec,
                                                              artifact,
                                                              RubyDependencyType.DEVELOPMENT);
        gem.addFile(rubyDevelopmentStubFile, rubyDevelopmentStubPath);

        // write file
        final File gemfile = this.gemPackager.createGem(gem, target);

        return new GemArtifact(gemspec, gemfile);
    }

    // ==

    protected String sanitizeStringValue(final String val) {
        if (val == null) {
            return null;
        }

        // for now, just to overcome the JRuby 1.4 Yaml parse but revealed by
        // this POM:
        // http://repo1.maven.org/maven2/org/easytesting/fest-assert/1.0/fest-assert-1.0.pom
        return val.replaceAll("'", "").replaceAll("\"", "").replace('\n', ' ');
    }

    protected String createLibFileName(final MavenArtifact artifact,
            final String postfix) {
        return LIB_MAVEN_PATH
                + createRequireName(artifact.getCoordinates().getGroupId(),
                                    artifact.getCoordinates().getArtifactId(),
                                    artifact.getCoordinates().getVersion())
                + postfix;
    }

    protected String createRequireName(final String groupId,
            final String artifactId, final String version) {
        return groupId + "/" + artifactId;
    }

    protected String createJarfileName(final String groupId,
            final String artifactId, final String version) {
        return artifactId + ".jar";
    }

    protected String createGemName(final String groupId,
            final String artifactId, final String version) {
        // TODO: think about this
        return GEMNAME_PREFIX + groupId + GemManager.GROUP_ID_ARTIFACT_ID_SEPARATOR + artifactId;
    }

    protected String getGemFileName(final String groupId,
            final String artifactId, final String version, final String platform) {
        final String gemName = createGemName(groupId, artifactId, version);

        final String gemVersion = createGemVersion(version);

        return Gem.constructGemFileName(gemName, gemVersion, platform);
    }

    protected String getGemFileName(final GemSpecification gemspec) {
        return Gem.constructGemFileName(gemspec.getName(), gemspec.getVersion()
                .getVersion(), gemspec.getPlatform());
    }

    protected String createGemVersion(final String mavenVersion)
            throws NullPointerException {
        return this.maven2GemVersionConverter.createGemVersion(mavenVersion);
    }

    // ==

    private File generateRubyMetaStub(final GemSpecification gemspec,
            final MavenArtifact artifact) throws IOException {
        final VelocityContext context = new VelocityContext();
        context.put("gemVersion", gemspec.getVersion().getVersion());
        context.put("groupId", artifact.getCoordinates().getGroupId());
        context.put("artifactId", artifact.getCoordinates().getArtifactId());
        context.put("type", artifact.getPom().getPackaging());
        context.put("version", artifact.getCoordinates().getVersion());
        if (artifact.getArtifactFile() != null) {
            context.put("filename", createJarfileName(artifact.getCoordinates()
                                                              .getGroupId(),
                                                      artifact.getCoordinates()
                                                              .getArtifactId(),
                                                      artifact.getCoordinates()
                                                              .getVersion()));
        }
        final List<String> packageParts = new ArrayList<String>();

        for (final String part : artifact.getCoordinates()
                .getGroupId()
                .split("\\.")) {
            packageParts.add(titleize(part));
        }
        packageParts.add(titleize(artifact.getCoordinates().getArtifactId()));
        context.put("packageParts", packageParts);

        return generateRubyFile("metafile", context, "rubyMetaStub");
    }

    public static class MavenDependency {
        public String       name;
        public List<String> exclusions = new ArrayList<String>();

        public String getName() {
            return this.name;
        }

        public String getExclusions() {
            final StringBuilder buf = new StringBuilder();
            for (final String ex : this.exclusions) {
                buf.append(",'").append(ex).append("'");
            }
            return this.exclusions.size() > 0 ? buf.substring(1) : "";
        }
    }

    private File generateRubyStub(final GemSpecification gemspec,
            final MavenArtifact artifact, final RubyDependencyType type)
            throws IOException {
        final VelocityContext context = new VelocityContext();
        switch (type) {
        case RUNTIME:
            if (artifact.getArtifactFile() != null) {
                context.put("jarfile",
                            createJarfileName(artifact.getCoordinates()
                                    .getGroupId(), artifact.getCoordinates()
                                    .getArtifactId(), artifact.getCoordinates()
                                    .getVersion()));
            }
            break;
        case DEVELOPMENT:
            context.put("filename", artifact.getCoordinates().getArtifactId()
                    + ".rb");
            break;
        }
        final List<MavenDependency> deps = new ArrayList<MavenDependency>();
        for (final Dependency dependency : artifact.getPom().getDependencies()) {
            if (RubyDependencyType.toRubyDependencyType(dependency.getScope()) == type
                    && !dependency.isOptional()) {
                final MavenDependency mavenDependency = new MavenDependency();
                mavenDependency.name = createRequireName(dependency.getGroupId(),
                                                         dependency.getArtifactId(),
                                                         dependency.getVersion());
                for (final Exclusion exclusion : dependency.getExclusions()) {
                    mavenDependency.exclusions.add(exclusion.getGroupId() + "/"
                            + exclusion.getArtifactId());
                }
                deps.add(mavenDependency);
            }

        }
        context.put("dependencies", deps);

        return generateRubyFile("require" + type.name(), context, "rubyStub"
                + type.name());
    }

    private File generateRubyFile(final String templateName,
            final Context context, final String stubFilename)
            throws IOException {
        final InputStream input = getClass().getResourceAsStream("/"
                + templateName + ".rb.vm");

        if (input == null) {
            throw new FileNotFoundException(templateName + ".rb.vm");
        }

        final String rubyTemplate = IOUtil.toString(input);

        final File rubyFile = File.createTempFile(stubFilename, ".rb.tmp");

        final FileWriter fw = new FileWriter(rubyFile);

        this.velocityComponent.getEngine().evaluate(context,
                                                    fw,
                                                    "ruby",
                                                    rubyTemplate);

        fw.flush();

        fw.close();

        return rubyFile;
    }

    private String titleize(final String string) {
        final String[] titleParts = string.split("[-._]");
        final StringBuilder titleizedString = new StringBuilder();
        for (final String part : titleParts) {
            if (part != null && part.length() != 0) {
                titleizedString.append(StringUtils.capitalise(part));
            }
        }
        return titleizedString.toString();
    }

    private GemDependency convertDependency(final MavenArtifact artifact,
            final Dependency dependency) {
        final GemDependency result = new GemDependency();

        result.setName(createGemName(dependency.getGroupId(),
                                     dependency.getArtifactId(),
                                     dependency.getVersion()));

        result.setType(RubyDependencyType.toRubyDependencyType(dependency.getScope())
                .toString());

        final GemRequirement requirement = new GemRequirement();

        // TODO: we are adding "hard" dependencies here, but we should maybe
        // support Maven ranges too
        // based on
        // http://blog.zenspider.com/2008/10/rubygems-howto-preventing-cata.html
        final String version = createGemVersion(getDependencyVersion(artifact,
                                                                     dependency));
        final int index = version.length()
                - version.replaceFirst("^[^.]+[.][^.]+", "").length();
        requirement.addRequirement("~>",
                                   new GemVersion(version.substring(0, index)));

        result.setVersion_requirement(requirement);

        return result;
    }

    private String getDependencyVersion(final MavenArtifact artifact,
            final Dependency dependency) {
        if (dependency.getVersion() != null) {
            return dependency.getVersion();
        }
        else if (StringUtils.equals(artifact.getCoordinates().getGroupId(),
                                    dependency.getGroupId())) {
            // hm, this is same groupId, let's suppose they have same
            // dependency!
            return artifact.getCoordinates().getVersion();
        }
        else {
            // no help here, just interpolated POM
            return "unknown";
        }
    }

    public File createGemspecFromArtifact(final MavenArtifact artifact,
            final File target) throws IOException {
        final GemSpecification gemspec = createSpecification(artifact);
        final File targetFile = new File(target, getGemFileName(gemspec)
                + "spec");

        FileWriter writer = null;
        try {
            writer = new FileWriter(targetFile);
            writer.append(this.gemSpecificationIO.write(gemspec));
        }
        finally {
            IOUtil.close(writer);
        }
        return targetFile;
    }
}
