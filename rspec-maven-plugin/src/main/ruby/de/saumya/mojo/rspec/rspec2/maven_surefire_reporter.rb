
require 'rspec/core/formatters/base_formatter'
require 'pp'
require 'stringio'
require 'fileutils'

class MavenSurefireReporter < RSpec::Core::Formatters::BaseFormatter
  class MojoLog

    def info(str)
      puts str
    end  
    
  end
  
  MOJO_LOG = MojoLog.new

  class FileBatch
    attr_accessor :file
    attr_accessor :passing
    attr_accessor :failing
    attr_accessor :pending
    attr_accessor :started_at
    
    def initialize()
      @file = nil
      
      @passing = []
      @failing = []
      @pending = []
      
      @started_at = Time.now
      @stopped_at = nil
      
    end
    
    def stop!
      @stopped_at = Time.now
    end
    
    def duration
      @stopped_at - @started_at
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
  end
  
  def emit(io, &block)
    Emitter.new( io, &block )
  end
  
  class Emitter
    def initialize(io, &block)
      @io = io
      @indent = 0
      decl
      instance_eval &block
    end
    
    def decl
      @io.puts( %q(<?xml version="1.0" encoding="UTF-8" ?>) )
    end
    
    def tag(tag_name, attrs={}, &block)
      @io.puts( indent + "<#{tag_name} #{attributes(attrs)}>" )
      @indent = @indent + 1
      instance_eval &block if block
      @indent = @indent - 1
      @io.puts( indent + "</#{tag_name}>" )
    end
    
    def attributes(attrs)
      attrs.entries.map{|e| e.first.to_s + "=" + quote(e.last.to_s) }.join( ' ' )
    end
    
    def quote(str)
      %q(") + str.gsub( /"/, '&quot;' ) + %q(")
    end
    
    def indent()
      "    " * @indent
    end 
  end
  
  def finish_batch()
    return if ( @current_batch.nil? )
    @current_batch.stop!
    num_passing = @current_batch.passing.size
    num_failing = @current_batch.failing.size
    num_pending = @current_batch.pending.size
    num_tests   = num_passing + num_failing + num_pending
    duration    = @current_batch.duration 
    
    basename = File.basename( @current_batch.relative_file, ".rb" )
    report_file = File.join( self.output, "surefire-reports", "TEST-" + basename + ".xml" )
    
    FileUtils.mkdir_p( File.dirname( report_file ) )
    
    batch = @current_batch
    
    File.open( report_file, 'w' ) do |file|
      emit(file) do
        tag( :testsuite, :name=>basename, :time=>duration, :failures=>num_failing, :skipped=>num_pending, :errors=>0, :tests=>num_tests ) do
          batch.passing.each do |testcase|
            tag( :testcase, :name=>testcase.metadata[:description], :time=>0.0 )
          end
          batch.failing.each do |testcase|
            tag( :testcase, :name=>testcase.metadata[:description], :time=>0.0 ) do
              tag( :failure )
            end
          end
          batch.pending.each do |testcase|
            tag( :testcase, :name=>testcase.metadata[:description], :time=>0.0 ) do
              tag( :skipped )
            end
          end
        end
      end
    end
      
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
  end
  
end
