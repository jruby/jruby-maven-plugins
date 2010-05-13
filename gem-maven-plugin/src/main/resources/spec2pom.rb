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
  <name><![CDATA[#{spec.summary}]]></name>
  <description><![CDATA[#{spec.description}]]></description>
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
  left_version = nil
  right_version = nil
  version = nil
  (0..(dep.version_requirements.requirements.size - 1)).each do |index|
    req = dep.version_requirements.requirements[index]
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
      pre_version = gem_version.sub(/[.][0-9]+$/, '')
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

  spec_tuples = fetcher.find_matching dep, true, false, nil
  is_java = spec_tuples.last[0][2] == 'java' unless spec_tuples.empty?
  require 'net/http'
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
#  end
end

puts <<-POM
  </dependencies>
</project>
POM
