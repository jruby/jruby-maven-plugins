#-*- mode: ruby -*-
Gem::Specification.new do |s|
  s.name = 'no-deps'
  s.version = '1.0.0'
  s.summary = 'no dependencies project'
  s.description = 'a no dependencies project to demonstrat how gemspec2pom works'
  s.homepage = 'http://example.com'
  s.authors = ['Krysh Sample']
  s.email = ['k@example.com']
  s.licenses += ['AGPL']
  s.autorequire = 'my'
  s.bindir = 'mybin'
  s.default_executable = 'myexe'
  s.executables = ['hello']
  s.extensions = ['myext']
  s.extra_rdoc_files = ['README.txt']
  s.files = ['AGPL.txt','README.txt','test/first_test.rb','mybin/hello','myext','lib/first.rb','spec/first_spec.rb','features/first.feature']
  s.platform = 'java'
  s.post_install_message = 'be happy'
  s.rdoc_options = ['--main','README.txt']
  s.require_paths = ['mylib']
  s.required_ruby_version = '= 1.8.7'
  s.required_rubygems_version = '= 1.4.2'
  s.requirements = ['java']
  s.rubyforge_project = 'myproject'
  s.test_files = ['test/first_test.rb']
end
