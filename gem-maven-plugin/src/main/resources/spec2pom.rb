require 'rubygems'
require 'rubygems/format'

spec = Gem::Format.from_file_by_path(ARGV[0]).spec

puts <<-POM
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
  <name>#{spec.summary}</name>
  <description>#{spec.description}</description>
  <url>#{spec.homepage}</url>
  <dependencies>
POM

fetcher = Gem::SpecFetcher.fetcher

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
  spec_tuples = fetcher.find_matching dep, true, false, nil
  if spec_tuples.empty?
    warn "#{dep} is empty: #{spec_tuples.inspect}" 
  else
    # TODO make version ranges when applicable
    req = dep.version_requirements.requirements[0]
    gem_version = req[1].to_s
    gem_version = gem_version + ".0" if gem_version =~ /^[0-9]+\.[0-9]+$/
    gem_version = gem_version + ".0.0" if gem_version =~ /^[0-9]+$/
    case req[0]
    when "="
      version = gem_version
    when ">="
      version = "[#{gem_version},]"
    when ">"
      version = "(#{gem_version},)"
     when "<="
      version = "[,#{gem_version}]"
    when "<"
      version = "(,#{gem_version})"
    when "~>"
      version = "#{spec_tuples.first[0][1]}"
      #version = "[#{gem_version},#{gem_version.sub(/[.]*$/, '').to_i + 1}.0.0)"
    else
      puts "npt implemented comparator: #{req.inspect}"
      version = "[0.0.0,]"
    end
    warn "#{version} #{req.inspect}"
#    version = spec_tuples.last[0][1]
    is_java = spec_tuples.last[0][2] == 'java'
    puts <<-POM
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>#{dep.name}</artifactId>
      <version>#{version}</version>
      <type>gem</type>
POM
    if is_java
      puts <<-POM
      <classifier>java</classifier>
POM
    end
    puts <<-POM
      <scope>#{scope}</scope>
    </dependency>
POM
  end
end

puts <<-POM
  </dependencies>
</project>
POM
