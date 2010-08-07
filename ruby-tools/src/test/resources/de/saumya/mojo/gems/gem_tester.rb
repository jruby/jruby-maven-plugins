def setup_gems( gem_path )
  ENV['GEM_PATH'] = gem_path
  ENV['GEM_HOME'] = gem_path
  require 'rubygems'
end
  
def install_gems( *gem_names )
  require 'rubygems/dependency_installer'
  gems = Gem::DependencyInstaller.new( :domain => :local )
  gem_names.each do |gem_name|
    gems.install( gem_name )
  end
end

def require_gem( gem_name )
  require gem_name
end

self