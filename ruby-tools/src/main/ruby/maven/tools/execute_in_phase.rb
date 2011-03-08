require File.join(File.dirname(__FILE__), 'gem_project.rb')

proj = Maven::Tools::GemProject.new("in_phase_execution")

proj.load(ARGV[0])

block = proj.executions_in_phase[ARGV[1]]
block.call if block

