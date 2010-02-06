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

  unless spec_tuples.empty?
    # TODO make version ranges when applicable
    version = spec_tuples.last[0][1]
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
  end
  
  puts <<-POM
      <scope>#{scope}</scope>
    </dependency>
POM
end

puts <<-POM
  </dependencies>
</project>
POM
