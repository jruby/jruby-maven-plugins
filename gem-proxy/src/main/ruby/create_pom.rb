require 'java'

java_import 'de.saumya.mojo.ruby.ScriptUtils'

require ScriptUtils.getScriptFromResource('maven/tools/gem_project.rb').to_s
require ScriptUtils.getScriptFromResource('maven/tools/minimal_project.rb').to_s
require 'rubygems'
require 'rubygems/format'

class CreatePom
  
  def create(gemfile)
    #maven = Maven::Tools::GemProject.new
    #maven.load_gemspec spec(gemfile)
    #maven.add_defaults(:jruby_version => nil)
    maven = Maven::Tools::MinimalProject.new( spec( gemfile ) )
    maven.to_xml
  end

  def spec(gemfile)
    Gem::Format.from_file_by_path(gemfile).spec
  end
end

CreatePom.new
