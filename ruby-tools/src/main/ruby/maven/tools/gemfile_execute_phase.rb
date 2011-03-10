require File.join(File.dirname(__FILE__), 'gemfile_reader.rb')

warn "deprecated, use it with 'Mavenfile' instead"

gemfile = Maven::Tools::GemfileReader.new(File.new(ARGV[0]))
gemfile.execute_phase(ARGV[1])

