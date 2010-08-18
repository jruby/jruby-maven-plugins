#patched my rail3-maven-plugin to allow custom Gemfile names
require 'rubygems'

if defined?(JRUBY_VERSION)
  require 'java'
  ENV['BUNDLE_GEMFILE'] ||= java.lang.System.getProperty("bundle.gemfile")
  gemfile = ENV['BUNDLE_GEMFILE'].to_s
  File.delete(gemfile + ".lock") if File.exist?(gemfile + ".lock")
end

# Set up gems listed in the Gemfile.
gemfile = File.expand_path('../../Gemfile', __FILE__) unless File.exist?(gemfile)
begin
  ENV['BUNDLE_GEMFILE'] = gemfile
  require 'bundler'
  Bundler.setup
rescue Bundler::GemNotFound => e
  STDERR.puts e.message
  STDERR.puts "Try running `bundle install`." if defined?(JRUBY_VERSION) && java.lang.System.getProperty("maven.home").nil? || !defined?(JRUBY_VERSION)
  exit!
end if File.exist?(gemfile) || ENV['BUNDLE_GEMFILE']
