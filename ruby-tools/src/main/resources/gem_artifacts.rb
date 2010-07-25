require 'rubygems'
require 'fileutils'
require 'digest/sha1'

require 'rubygems/remote_fetcher'
class Gem::RemoteFetcher
  alias :fetch_path_old :fetch_path
  def fetch_path(uri, mtime = nil, head = false)
    begin
      fetch_path_old(uri, mtime, head)
    rescue FetchError => e
      warn e.message
      nil
    end
  end
end

module Maven
  class LocalRepository

    attr_reader :tuples

    def initialize(name = '', repository_id = 'rubygems', source_uri = "rubygems.org", local_repository = nil)
      @repository_id = repository_id
      if local_repository.nil?
        if File.exists?("/var/cache/gem-proxy")
          @local_repo = "/var/cache/gem-proxy"
        elsif File.exists?("./target")
          @local_repo = "./target/gem-proxy"
          FileUtils.makedirs(@local_repo)
        else
          @local_repo = File.join(Gem.user_home, ".m2", "repository", "rubygems")
        end
      else
        @local_repo = local_repository
      end
      @source_uri = source_uri
      @fetcher = Gem::SpecFetcher.fetcher
    end

    def find(fetcher, dep, prerelease)
      # all = !req.prerelease? since "all == true" excludes prereleases
      # all == true => prerelease == false
      # all == false => only latest !! unless prerelease == true
      platform = true # assume that code will run on jruby
      fetcher.find_matching(dep, !prerelease, platform, prerelease)
    end

    def update(name = '', update = true)
      dep = Gem::Dependency.new(/^#{name}/, Gem::Requirement.default)
      dep.name = '' if dep.name == //
      fetcher = Gem::SpecFetcher.new
      fetcher.instance_variable_set(:@update_cache, update)
      #TODO not sure if this is threadsafe
      @tuples = find(fetcher, dep, false) + find(fetcher, dep, true)
      @fetcher = fetcher
      @last_update = File.new(@fetcher.cache_dir(URI.parse("http://rubygems.org/gems")) + "/specs.#{Gem.marshal_version}").mtime
    end

    def spec(name, version)
      req = Gem::Requirement.new(version)
      dep = Gem::Dependency.new(/^#{name}$/, req)

      tuples = find(@fetcher, dep, req.prerelease?)
      unless tuples.empty?
        tuple = tuples.first
        @fetcher.fetch_spec(tuple[0], URI.parse(tuple[1]))
      end
    end

    def to_pom(name, version)
      pom = pom_file(name, version)
      if File.exists?(pom)
        pom
      else
        spec = spec(name, version)
        if spec
          spec_to_pom(spec, pom)
          pom
        else
          nil
        end
      end
    end

    def spec_to_pom(spec, pom)
      File.open(pom, "w") do |f|
        _spec_to_pom(spec, f)
      end
      sha1(pom)
    end

    def to_pomxml(specfile)
      spec = Gem::Specification.load(specfile)
      result = StringIO.new
      _spec_to_pom(spec, result)
      result.string
    end

    def _spec_to_pom(spec, f)
        f.puts <<-POM
<?xml version="1.0"?>
<project
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
    xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>rubygems</groupId>
  <artifactId>#{spec.name}</artifactId>
  <version>#{spec.version}</version>
  <packaging>gem</packaging>
  <name><![CDATA[#{spec.summary}]]></name>
  <description><![CDATA[#{spec.description}]]></description>
  <url>#{spec.homepage}</url>
  <developers>
POM
      (spec.email || []).zip(spec.authors || []).map do |e, a|     
        f.puts <<-POM
    <developer>
      <id>#{e.sub(/@.*/, '')}</id>
      <name>#{a}</name>
      <email>#{e}</email>
    </developer>
POM
      end
      f.puts <<-POM
  </developers>
  <licenses>
POM
#  TODO work with collection of licenses - there can be more than one !!!
# TODO make this better, i.e. detect the right license name from the file itself
      license = spec.files.detect {|file| file.to_s =~ /license/i }
      unless license.nil?
        f.puts <<-POM
    <license>
      <name>#{File.basename(license)}</name>
      <url>./#{license.sub(/^.\//,'')}</url>
      <distribution>repo</distribution>
    </license>
 POM
      end
      f.puts <<-POM
  </licenses>
  <dependencies>
POM
        spec.dependencies.each do |dep|
          scope = case dep.type
                  when :runtime
                      "compile"
                  when :development
                    "test"
                  else
                    warn "unknown scope: #{dep.type}"
                    "compile"
                  end
          left_version = nil
          right_version = nil
          version = nil
          (0..(dep.requirement.requirements.size - 1)).each do |index|
            req = dep.requirement.requirements[index]
            gem_version = req[1].to_s
            gem_final_version = gem_version
            gem_final_version = gem_final_version + ".0" if gem_final_version =~ /^[0-9]+\.[0-9]+$/
            gem_final_version = gem_final_version + ".0.0" if gem_final_version =~ /^[0-9]+$/
            case req[0]
            when "="
              version = gem_final_version
            when ">="
              left_version = "[#{gem_final_version}"
            when ">"
              left_version = "(#{gem_final_version}"
            when "<="
              right_version = "#{gem_final_version}]"
            when "<"
              right_version = "#{gem_final_version})"
            when "~>"
              # TODO not sure if this makes sense for prereleases
              pre_version = gem_version.sub(/[.][a-zA-Z0-9]+$/, '')
              # hope the upper bound is "big" enough but needed, i.e.
              # version 4.0.0 is bigger than 4.0.0.pre and [3.0.0, 4.0.0) will allow
              # 4.0.0.pre which is NOT intended
              version = "[#{gem_version},#{pre_version.sub(/[0-9]+$/, '')}#{pre_version.sub(/.*[.]/, '').to_i}.99999.99999)"
            else
              puts "not implemented comparator: #{req.inspect}"
            end
          end
          warn "having left_version or right_version and version which does not makes sense" if (right_version || left_version) && version
          version = (left_version || "[") + "," + (right_version || ")") if right_version || left_version
          version = "[0.0.0,)" if version.nil?

          spec_tuples = @fetcher.find_matching dep, true, false, nil
          is_java = spec_tuples.detect { |s| s[0][2] == 'java' }
          # require 'net/http'
          f.puts <<-POM
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>#{dep.name}</artifactId>
      <version>#{version}</version>
      <type>gem</type>
POM
          if is_java
            f.puts <<-POM
      <classifier>java</classifier>
POM
          end
          f.puts <<-POM
      <scope>#{scope}</scope>
    </dependency>
POM
        end
        f.puts <<-POM
  </dependencies>
  <repositories>
    <repository>
      <id>rubygems-releases</id>
      <url>http://gems.saumya.de/releases</url>
    </repository>
  </repositories>
  <pluginRepositories>
    <pluginRepository>
      <id>sonatype-nexus-snapshots</id>
      <name>Sonatype Nexus Snapshots</name>
      <url>http://oss.sonatype.org/content/repositories/snapshots</url>
    </pluginRepository>
  </pluginRepositories>
  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <jruby.plugins.version>0.20.0-SNAPSHOT</jruby.plugins.version>
  </properties>
  <build>
    <plugins>
      <plugin>
        <groupId>de.saumya.mojo</groupId>
        <artifactId>gem-maven-plugin</artifactId>
        <version>${jruby.plugins.version}</version>
        <extensions>true</extensions>
        <executions>
          <execution>
            <goals><goal>pom</goal></goals>
            <configuration>
              <date>#{spec.date.strftime("%Y-%m-%d")}</date>
POM
      _add_tag(f, "extraRdocFiles", spec.extra_rdoc_files.join(',').to_s)
      _add_tag(f, "rdocOptions", spec.rdoc_options.join(',').to_s)
      _add_tag(f, "requirePaths", spec.require_paths.join(',').to_s) if  spec.require_paths.join(',').to_s != "lib"
      _add_tag(f, "rubyforgeProject", spec.rubyforge_project.to_s)
      _add_tag(f, "rubygemsVersion", spec.rubygems_version.to_s)
      _add_tag(f, "requiredRubygemsVersion", spec.required_rubygems_version.to_s) if spec.required_rubygems_version.to_s != ">= 0"
      _add_tag(f, "bindir", spec.bindir.to_s) if spec.bindir.to_s != "bin"
      _add_tag(f, "requiredRubyVersion", spec.required_ruby_version.to_s) if spec.required_ruby_version.to_s != ">= 0"
      _add_tag(f, "postInstallMessage", spec.post_install_message.to_s)
      _add_tag(f, "executables", spec.executables.to_s)
      _add_tag(f, "extensions", spec.extensions.to_s)
      _add_tag(f, "platform", spec.platform.to_s) if spec.platform != 'ruby'
      _extra_files(f, spec)
      f.puts <<-POM
            </configuration>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>de.saumya.mojo</groupId>
        <artifactId>rspec-maven-plugin</artifactId>
        <version>${jruby.plugins.version}</version>
        <executions>
          <execution>
            <goals><goal>test</goal></goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>2.0.2</version>
        <configuration>
          <source>1.5</source>
          <target>1.5</target>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
POM
    end

    def _add_tag(io, tag, value)
      if value && value.size > 0
        io.puts "              <#{tag}>#{value}</#{tag}>"
      end
    end

    def _extra_files(io, spec)
      files = spec.files.dup
      (Dir['lib/**/*'] + Dir['generators/**/*'] + Dir['spec/**/*'] + spec.licenses + spec.extra_rdoc_files).each do |f|
        files.delete(f)
        if f =~ /^.\//
          files.delete(f.sub(/^.\//, ''))
        else
          files.delete("./#{f}")
        end
      end
      _add_tag(io, "extra_files", files.join(","))
    end

    def pom_file(name, version)
      dir = File.join(@local_repo, name, version)
      FileUtils.makedirs(dir)
      File.join(dir, "#{name}-#{version}.pom")
    end

    def pom_sha1_file(name, version)
      file = pom_file(name, version) + ".sha1"
      unless File.exists?(file)
        return nil if spec_to_pom(name, version).nil?
      end
      file
    end

    def gem_details(name,version)
      req = Gem::Requirement.new(version)
      dep = Gem::Dependency.new(/^#{name}$/, req)

      tuples = find(@fetcher, dep, req.prerelease?)
      return nil if tuples.empty?
      tuples.detect {|t| t[0][2] == 'java' } || tuples.first
    end

    def gem_location(name, version)
      tuple = gem_details(name, version)
      return nil if tuple.nil?
      "#{tuple[1]}/gems/#{name}-#{version}" +
        ("java" == tuple[0][2] ? "-java" : "") + ".gem"
    end

    def gem_sha1_file(name, version)
      file = pom_file(name, version)
      return nil if file.nil?
      file = file.sub(/\.pom$/, ".gem") + ".sha1"
      unless File.exists?(file)
        require 'net/http'
        tuple = gem_details(name, version)
        resource = Net::HTTP.new(tuple[1].sub(/http:../,80))
        headers,data = resource.get("/gems/#{name}-#{version}" +
                                    ("java" == tuple[0][2] ? "-java" : "") +
                                    ".gem")
        if(headers.code == "302")
          # follow one redirect
          domain = headers["location"].sub(/.*:\/\//, '').sub(/\/.*/, '')
          path = headers["location"].sub(/.*:\/\/[^\/]*\//, '/')
          resource = Net::HTTP.new(domain,80)
          headers,data = resource.get(path)
          # TODO do it better here
          return nil if(headers.code != "200")
        end
        File.open(file, "w") { |f| f << Digest::SHA1.hexdigest(data) }
      end
      file
    end

    def sha1(file)
      c = ""
      File.new(file).each { |l| c << l }
      File.open(file + ".sha1", "w") { |f| f << Digest::SHA1.hexdigest(c) }
    end

    def map
      map = {}
      current = nil
      versions = nil
      @tuples.each do |tuple|
        if tuple[0][2] =~ /ruby|java/
          if current != tuple[0][0]
            current = tuple[0][0]
            versions = (map[current] ||= [])
          end
          versions << tuple[0][1].to_s
        end
      end
      map
    end

    def metadata(name, prereleases = false)
      versions = map[name]
      if versions
        if prereleases
          versions = versions.select {|v| v =~ /[a-zA-Z]/ }
        else
          versions = versions.select {|v| v =~ /^[0-9.]+$/ }
        end
        to_metadata(name, versions, prereleases)
      end
    end

    def metadata_file(name, prereleases)
      dir = File.join(@local_repo, name)
      FileUtils.makedirs(dir)
      File.join(dir, "maven-metadata-#{@repository_id}#{prereleases ? "-prereleases" : "-releases"}.xml")
    end

    def metadata_sha1_file(name, prereleases = false, is_sha1 = false)
      file = metadata_file(name, prereleases) + ".sha1"
      unless File.exists?(file)
        return nil if metadata(name).nil?
      end
      file
    end

    def to_metadata(name, versions, prereleases = false, create_sha1 = true)
      metadata = metadata_file(name, prereleases)
      if !File.exists?(metadata) # || (File.new(metadata).mtime < @last_update)
        @count ||= 0
        print "." if @count % 10 == 0
        if(@count == 800)
          puts
          @count = 0
        else
          @count = @count + 1
        end
        File.open(metadata, "w") do |f|
          f.puts <<-METADATA
<?xml version="1.0" encoding="UTF-8"?>
<metadata>
  <groupId>rubygems</groupId>
  <artifactId>#{name}</artifactId>
  <versioning>
    <versions>
METADATA
          versions.each do |v|
            f.puts <<-METADATA
      <version>#{v}</version>
METADATA
          end
          f.puts <<-METADATA
    </versions>
    <lastUpdated>#{Time.now.strftime("%Y%m%d%H%M%S")}</lastUpdated>
  </versioning>
</metadata>
METADATA
        end
        # puts "#{@last_update} #{metadata}"
        File.utime(@last_update, @last_update, metadata)
      end
      sha1(metadata) if create_sha1
      metadata
    end

    def update_all_metadata(prerelease)
      update
      map.each do |name, versions|
        to_metadata(name, versions, prerelease, false)
      end
    end
  end
end

Maven::LocalRepository.new()
