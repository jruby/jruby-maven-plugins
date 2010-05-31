require 'rubygems'
require 'fileutils'
require 'digest/sha1'

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
          @local_repo = File.join(Gem.user_home, ".m2", "repository", "rubygems-test")
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
      fetcher.find_matching(dep, !prerelease, false, prerelease)
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
          _spec_to_pom(spec, pom)
          pom
        else
          nil
        end
      end
    end
    
    def spec_to_pom(spec, pom)
      #TODO spec file to Gem::Specification
      _spec_to_pom(spec, pom)
    end
    
    def _spec_to_pom(spec, pom)
      File.open(pom, "w") do |f|
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
</project>
POM
      end
      sha1(pom)
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

    def gem_location(name, version, filename)
      file = pom_file(name, version)
      # TODO maybe a better way to check if name + version is valid
      return nil if file.nil?
      "http://#{@source_uri}/gems/#{filename}"
    end

    def gem_sha1_file(name, version)
      file = pom_file(name, version)
      return nil if file.nil?
      file = file.sub(/\.pom$/, ".gem") + ".sha1"
      unless File.exists?(file)
        require 'net/http'
        resource = Net::HTTP.new(@source_uri,80)
        headers,data = resource.get("/gems/#{name}-#{version}.gem")
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
      File.join(dir, "maven-metadata-#{@repository_id}#{prereleases ? "-prereleases" : ""}.xml")
    end

    def metadata_sha1_file(name, prereleases = false, is_sha1 = false)
      file = metadata_file(name, prereleases) + ".sha1"
      unless File.exists?(file)
        return nil if metadata(name).nil?
      end
      file
    end

    def to_metadata(name, versions, prereleases = false)
      metadata = metadata_file(name, prereleases)
      if !File.exists?(metadata) || (File.new(metadata).mtime < @last_update)
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
      sha1(metadata)
      metadata
    end
    
    def update_all_metadata
      map.each do |name, versions|
        to_metadata(name, versions)
      end
    end
  end
end

Maven::LocalRepository.new()
