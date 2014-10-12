require 'rubygems/installer'
require 'fileutils'

Dir[ 'target/dependency/*gem' ].each do |file|
  installer = Gem::Installer.new( file,
                                  :ignore_dependencies => true,
                                  :install_dir => '../target/rubygems' )
  installer.install
end

Dir[ 'target/dependency/*gem' ].each do |file|
  f = "../target/rubygems/gems/#{File.basename(file).sub(/.gem/, '')}/lib"
  unless File.exists?( f )
    f = "../target/rubygems/gems/#{File.basename(file).sub(/.gem/, '')}-java/lib"
  end
  Dir[ File.expand_path( f ) + "/*" ].each do |ff|
    FileUtils.cp_r( "#{ff}", 'target/classes' )
  end
end
