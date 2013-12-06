require 'java'

java_import 'de.saumya.mojo.ruby.ScriptUtils'

require ScriptUtils.getScriptFromResource('maven/tools/pom.rb').to_s

require 'rubygems/package'

class CreatePom
  
  def create(gemfile)
    Maven::Tools::POM.new( Gem::Package.new( gemfile ).spec ).to_s
  end

end

CreatePom.new
