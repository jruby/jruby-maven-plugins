require 'rubygems/installer'
require 'fileutils'

Dir[ 'target/dependency/*gem' ].each do |file|
  installer = Gem::Installer.new( file,
                                  :ignore_dependencies => true,
                                  :install_dir => 'target/rubygems' )
  installer.install
end

Dir[ 'target/rubygems/gems/*/lib/*' ].each do |f|
  FileUtils.cp_r( "#{f}", 'target/classes' )
end
