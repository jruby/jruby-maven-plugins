package de.saumya.mojo.gems.spec;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Gem::Specification
 * 
 * @author cstamas
 */
public class GemSpecification {
    private List<String>   authors;

    @Deprecated
    private String         autorequire;

    private String         bindir;

    private List<String>   cert_chain;

    private Date           date;

    private String         default_executable;

    private List<Object>   dependencies;

    private String         description;

    private String         email;

    private List<String>   executables;

    private List<String>   extensions;

    private List<String>   extra_rdoc_files;

    private List<String>   files;

    private boolean        has_rdoc;

    private String         homepage;

    private String         name;

    private String         platform;

    private List<String>   rdoc_options;

    private List<String>   require_paths;

    private GemRequirement required_ruby_version;

    private GemRequirement required_rubygems_version;

    private List<String>   requirements;

    private String         rubyforge_project;

    private String         rubygems_version;

    private String         specification_version;

    private String         summary;

    private List<String>   test_files;

    private GemVersion     version;

    private List<String>   licenses;

    private String         post_install_message;

    private String         signing_key;

    public void setAuthor(final String author) {
        getAuthors().add(author);
    }

    public List<String> getAuthors() {
        if (this.authors == null) {
            this.authors = new ArrayList<String>();
        }

        return this.authors;
    }

    public void setAuthors(final List<String> authors) {
        this.authors = authors;
    }

    @Deprecated
    public String getAutorequire() {
        return this.autorequire;
    }

    @Deprecated
    public void setAutorequire(final String autorequire) {
        this.autorequire = autorequire;
    }

    public String getBindir() {
        return this.bindir;
    }

    public void setBindir(final String bindir) {
        this.bindir = bindir;
    }

    public List<String> getCert_chain() {
        if (this.cert_chain == null) {
            this.cert_chain = new ArrayList<String>();
        }

        return this.cert_chain;
    }

    public void setCert_chain(final List<String> certChain) {
        this.cert_chain = certChain;
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(final Date date) {
        this.date = date;
    }

    public String getDefault_executable() {
        return this.default_executable;
    }

    public void setDefault_executable(final String defaultExecutable) {
        this.default_executable = defaultExecutable;
    }

    public List<Object> getDependencies() {
        if (this.dependencies == null) {
            this.dependencies = new ArrayList<Object>();
        }

        return this.dependencies;
    }

    public void setDependencies(final List<Object> dependencies) {
        getDependencies().addAll(dependencies);
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public List<String> getExecutables() {
        if (this.executables == null) {
            this.executables = new ArrayList<String>();
        }

        return this.executables;
    }

    public void setExecutables(final List<String> executables) {
        this.executables = executables;
    }

    public List<String> getExtensions() {
        if (this.extensions == null) {
            this.extensions = new ArrayList<String>();
        }

        return this.extensions;
    }

    public void setExtensions(final List<String> extensions) {
        this.extensions = extensions;
    }

    public List<String> getExtra_rdoc_files() {
        if (this.extra_rdoc_files == null) {
            this.extra_rdoc_files = new ArrayList<String>();
        }

        return this.extra_rdoc_files;
    }

    public void setExtra_rdoc_files(final List<String> extraRdocFiles) {
        this.extra_rdoc_files = extraRdocFiles;
    }

    public void addExtraRdocFile(final String extraRdocFile) {
        getExtra_rdoc_files().add(extraRdocFile);
    }

    public List<String> getFiles() {
        if (this.files == null) {
            this.files = new ArrayList<String>();
        }

        return this.files;
    }

    public void addFile(final String file) {
        getFiles().add(file);
    }

    public void setFiles(final List<String> files) {
        this.files = files;
    }

    public boolean isHas_rdoc() {
        return this.has_rdoc;
    }

    public void setHas_rdoc(final boolean hasRdoc) {
        this.has_rdoc = hasRdoc;
    }

    public String getHomepage() {
        return this.homepage;
    }

    public void setHomepage(final String homepage) {
        this.homepage = homepage;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getPlatform() {
        return this.platform;
    }

    public void setPlatform(final String platform) {
        this.platform = platform;
    }

    public List<String> getRdoc_options() {
        if (this.rdoc_options == null) {
            this.rdoc_options = new ArrayList<String>();
        }

        return this.rdoc_options;
    }

    public void setRdoc_options(final List<String> rdocOptions) {
        this.rdoc_options = rdocOptions;
    }

    public List<String> getRequire_paths() {
        if (this.require_paths == null) {
            this.require_paths = new ArrayList<String>();
        }

        return this.require_paths;
    }

    public void setRequire_paths(final List<String> requirePaths) {
        this.require_paths = requirePaths;
    }

    public GemRequirement getRequired_ruby_version() {
        return this.required_ruby_version;
    }

    public void setRequired_ruby_version(
            final GemRequirement requiredRubyVersion) {
        this.required_ruby_version = requiredRubyVersion;
    }

    public GemRequirement getRequired_rubygems_version() {
        return this.required_rubygems_version;
    }

    public void setRequired_rubygems_version(
            final GemRequirement requiredRubygemsVersion) {
        this.required_rubygems_version = requiredRubygemsVersion;
    }

    public List<String> getRequirements() {
        if (this.requirements == null) {
            this.requirements = new ArrayList<String>();
        }

        return this.requirements;
    }

    public void setRequirements(final List<String> requirements) {
        this.requirements = requirements;
    }

    public String getRubyforge_project() {
        return this.rubyforge_project;
    }

    public void setRubyforge_project(final String rubyforgeProject) {
        this.rubyforge_project = rubyforgeProject;
    }

    public String getRubygems_version() {
        return this.rubygems_version;
    }

    public void setRubygems_version(final String rubygemsVersion) {
        this.rubygems_version = rubygemsVersion;
    }

    public String getSpecification_version() {
        return this.specification_version;
    }

    public void setSpecification_version(final String specificationVersion) {
        this.specification_version = specificationVersion;
    }

    public String getSummary() {
        return this.summary;
    }

    public void setSummary(final String summary) {
        this.summary = summary;
    }

    public void addTestFile(final String testFile) {
        getTest_files().add(testFile);
    }

    public List<String> getTest_files() {
        if (this.test_files == null) {
            this.test_files = new ArrayList<String>();
        }

        return this.test_files;
    }

    public void setTest_files(final List<String> testFiles) {
        this.test_files = testFiles;
    }

    public GemVersion getVersion() {
        return this.version;
    }

    public void setVersion(final GemVersion version) {
        this.version = version;
    }

    public List<String> getLicenses() {
        if (this.licenses == null) {
            this.licenses = new ArrayList<String>();
        }

        return this.licenses;
    }

    public void setLicenses(final List<String> licenses) {
        this.licenses = licenses;
    }

    public String getPost_install_message() {
        return this.post_install_message;
    }

    public void setPost_install_message(final String postInstallMessage) {
        this.post_install_message = postInstallMessage;
    }

    public String getSigning_key() {
        return this.signing_key;
    }

    public void setSigning_key(final String signingKey) {
        this.signing_key = signingKey;
    }

}
