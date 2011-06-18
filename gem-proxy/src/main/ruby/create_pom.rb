require 'java'

java_import 'de.saumya.mojo.ruby.ScriptUtils'

require ScriptUtils.getScriptFromResource('maven/tools/pom_generator.rb').to_s
require 'rubygems'
require 'rubygems/format'

class CreatePom
  
  def create(gemfile)
    maven = Maven::Tools::GemProject.new
    maven.load_gemspec Gem::Format.from_file_by_path(gemfile).spec
    maven.add_defaults(:jruby_version => nil)
    maven.to_xml
  end
end

CreatePom.new
