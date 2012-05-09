# -*- coding: utf-8 -*-
require File.expand_path('lib/maven/tools/version.rb')
Gem::Specification.new do |s|
  s.name = 'maven-tools'
  s.version = Maven::Tools::VERSION.dup

  s.summary = 'helpers for maven related tasks'

  s.authors = ['Kristian Meier']
  s.email = ['m.kristian@web.de']

  s.files += Dir['lib/**/*']
  s.files += Dir['spec/**/*']
  s.files += Dir['MIT-LICENSE'] + Dir['*.md']
  s.test_files += Dir['spec/**/*_spec.rb']
  s.add_development_dependency 'rake', '0.9.2.2'
  s.add_development_dependency 'minitest', '2.10.0'
end
