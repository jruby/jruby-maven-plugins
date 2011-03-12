Gem::Specification.new do |s|
  s.name = 'sample'
  s.version = '0.1.0'

  s.summary = 'summary of sample'
  s.description = 'description of sample'
  s.homepage = 'http://example.com/'

  s.authors = ['mkristian']
  s.email = ['m.kristian@web.de']

  s.files = Dir['MIT-LICENSE']
  s.licenses << 'MIT-LICENSE'
  s.files += Dir['lib/**/*']
  s.add_development_dependency 'rake', '0.8.7'

  s.post_install_message = <<-TEXT
- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
TEXT
end
