require 'rubygems'

name = ARGV[0]
puts <<-POM
<?xml version="1.0" encoding="UTF-8"?>
<metadata>
  <groupId>rubygems</groupId>
  <artifactId>#{name}</artifactId>
  <versioning>
    <versions>
POM

dep = Gem::Dependency.new(name, Gem::Requirement.default)

fetcher = Gem::SpecFetcher.fetcher

tuples = fetcher.find_matching(dep, true, false, false)
tuples = tuples + fetcher.find_matching(dep, false, false, true)

warn name
warn tuples.inspect

tuples.each do |tuple|
  puts <<-POM
      <version>#{tuple[0][1]}</version>
POM
end

puts <<-POM
    </versions>
    <lastUpdated>#{Time.now.strftime("%Y%m%d%H%M%S")}</lastUpdated>
  </versioning>
</metadata>
POM
