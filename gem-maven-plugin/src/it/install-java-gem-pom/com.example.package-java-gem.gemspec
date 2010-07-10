# create by maven - leave it as is
Gem::Specification.new do |s|
  s.name = 'com.example.package-java-gem'
  s.version = '1.0'

  s.summary = 'package java-gem'
  s.homepage = 'http://maven.apache.org'


  s.platform = 'java'
  s.files = Dir['lib/package-java-gem-1.0-SNAPSHOT.jar']
  s.files += Dir['lib/com.example.package-java-gem.rb']
end