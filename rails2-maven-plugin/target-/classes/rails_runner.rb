require 'rubygems'
version = '>= 0'
if ARGV.first =~ /^_(.*)_$/ and Gem::Version.correct? $1 then
  version = $1
  ARGV.shift
end
gem 'rails', version
load Gem.bin_path('rails', 'rails', version)
puts Gem.bin_path('rails', 'rails', version)

