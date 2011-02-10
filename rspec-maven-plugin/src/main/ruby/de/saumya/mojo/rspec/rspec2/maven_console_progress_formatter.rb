
require 'rspec/core/formatters/base_formatter'
require 'pp'

class MojoLog

  def info(str)
    puts str
  end  
  
end

MOJO_LOG = MojoLog.new

class MavenConsoleProgressFormatter < RSpec::Core::Formatters::BaseFormatter

  class FileBatch
    attr_accessor :file
    attr_accessor :passing
    attr_accessor :failing
    attr_accessor :pending
    
    def initialize()
      @file = nil
      
      @passing = []
      @failing = []
      @pending = []
      
    end
    
    def relative_file
      #return "unknown" if ( @file.nil? || @file == '' )
      #return @file if ( BASE_DIR.nil? || BASE_DIR == '' )
      
      #puts "file==[#{@file}]"
      #puts "BASE_DIR==[#{BASE_DIR}]"
      
      
      #file_pathname = Pathname.new( @file )
      #base_pathname = Pathname.new( BASE_DIR )
      
      #Pathname.new( @file ).relative_path_from( Pathname.new( BASE_DIR ) ) 
      @file
    end
  end
  
  def initialize(output)
    super( output )
    @batches       = []
    @current_batch = nil
  end
  
  def example_group_started(example_group)
  end
  
  def example_started(example)
    file, lineno = example.metadata.file_and_line_number
    
    if ( @current_batch.nil? || @current_batch.file != file )
      start_new_batch(example)
      @current_file = file
    end
  end
  
  def example_passed(example)
    @current_batch.passing << example
  end
  
  def example_failed(example)
    @current_batch.failing << example
  end
  
  def example_pending(example)
    @current_batch.pending << example
  end
  
  def start_new_batch(example)
    finish_batch()
    @current_batch = FileBatch.new
    
    file, lineno = example.metadata.file_and_line_number
    @current_batch.file =  file
    
    puts "* SPEC: #{@current_batch.relative_file}"
      
  end
  
  def finish_batch()
    return if ( @current_batch.nil? )
    num_passing = @current_batch.passing.size
    num_failing = @current_batch.failing.size
    num_pending = @current_batch.pending.size
    num_tests   = num_passing + num_failing + num_pending
    
    message = "  Tests: #{sprintf("%3d", num_tests)}\n"
    
    if ( num_passing == num_tests )
      message += "    passing: ALL"
    else
      message += "    passing#{sprintf("%8d", num_passing).gsub(/ /, '.')}\n"
      if ( num_pending > 0 )
        message += "    pending#{sprintf("%8d", num_pending).gsub(/ /, '.')}\n"
      end
      if ( num_failing > 0 )
        message += "    failing#{sprintf("%8d", num_failing).gsub(/ /, '.')}\n"
      end
    end
    
    puts message.chomp
    @batches << @current_batch
    @current_batch = nil
  end
  
  def example_finished(example)
  end
  
  def example_group_finished(example_group)
  end
  
  def start_dump
    finish_batch
  end
  
  def dump_summary(duration, example_count, failure_count, pending_count)
    puts "============================================="
    
    if ( pending_count > 0 )
      puts ""
      puts "Pending specs:"
    
      @batches.each do |batch|
        if ( batch.failing.size > 0 )
          puts "  - #{batch.relative_file}"
          batch.pending.each do |pending|
            ignored_file, lineno = pending.metadata.file_and_line_number
            puts "    line #{lineno}: #{pending.full_description}"
          end
        end
      end
    end
    
    if ( failure_count > 0 )
      puts ""
      puts "Failing specs:"
      
      @batches.each do |batch|
        if ( batch.failing.size > 0 )
          puts "  - #{batch.relative_file}"
          batch.failing.each do |failing|
            ignored_file, lineno = failing.metadata.file_and_line_number
            puts "    line #{lineno}: #{failing.full_description}"
          end
        end
      end
    end
    
    puts ""
  end
  
=begin
  def dump_summary(duration, example_count, failure_count, pending_count)
    unless ( @first )
      MOJO_LOG.info( "  #{@passing.size} passing; #{@failing.size} failing; #{@pending.size} pending")
    end
    pass_count = example_count - ( failure_count + pending_count ) 
    MOJO_LOG.info( "=========================================" )
    MOJO_LOG.info( "TOTAL: #{pass_count} passing; #{failure_count} failing; #{pending_count} pending")
  end
=end
  
end