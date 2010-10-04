require File.join(File.dirname(__FILE__), 'gemfile_reader.rb')

# TODO usage
gemfile = Maven::Tools::GemfileReader.new(File.new(ARGV[0]))
gemfile.execute_phase(ARGV[1])

