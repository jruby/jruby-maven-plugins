require File.join(File.dirname(__FILE__), 'model.rb')

# TODO usage

proj = GemProject.new("in_phase_execution")

eval "proj.mergefile(File.new('#{ARGV[0]}'))"

proj.execute_in_phase(ARGV[1])

