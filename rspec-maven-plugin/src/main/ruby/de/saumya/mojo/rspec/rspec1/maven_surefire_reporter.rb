
require 'spec/runner/formatter/base_formatter'


class FileInfo
  attr_accessor :path
  attr_accessor :groups
  
  def initialize(path)
    @path = path
    @groups = []
  end
  
  def duration
    @groups.inject(0){|sum,e|sum+=e.duration}
  end
  
  def examples
    @groups.collect{|e| e.examples}.flatten
  end
  
  def passing
    @groups.collect{|e| e.passing}.flatten
  end
  
  def failing
    @groups.collect{|e| e.failing}.flatten
  end
  
  def pending
    @groups.collect{|e| e.pending}.flatten
  end
  
end

class GroupInfo
  attr_accessor :rspec_group
  attr_accessor :examples
  
  def initialize(rspec_group)
    @rspec_group = rspec_group
    @examples = []
  end
  
  def duration
    @examples.inject(0){|sum,e|sum+=e.duration}
  end
  
  def passing
    @examples.select{|e| e.status == :passing }
  end
  
  def failing
    @examples.select{|e| e.status == :failing }
  end
  
  def pending
    @examples.select{|e| e.status == :pending }
  end
  
end

class ExampleInfo
  attr_accessor :rspec_example
  attr_accessor :duration
  attr_accessor :status
  
  attr_accessor :failure
  attr_accessor :message
  
  def initialize(rspec_example)
    @rspec_example = rspec_example
    @duration = 0
    @start_time = Time.now
    @status = :pending
  end
  
  def finish()
    @duration = ( Time.now - @start_time ).to_f
  end
end


class MavenSurefireReporter < Spec::Runner::Formatter::BaseFormatter
  
  attr_accessor :output
  
  def initialize(options, output)
    super( options, output )
    @file_info = {}
    
    @output = output
    
    @current_file_info = nil
    @current_group_info = nil
    @current_example_info = nil
  end
  
  def files
    @file_info.values
  end
  
  def example_group_started(example_group)   
    setup_current_file_info( example_group )
    setup_current_group_info( example_group )
    @current_group = example_group
  end
  
  def setup_current_file_info(example_group)
    filename = filename_for( example_group )
    @current_file_info = @file_info[ filename ] 
    if ( @current_file_info.nil? )
      @current_file_info = FileInfo.new( filename )
      @file_info[ filename ] = @current_file_info
    end
  end
  
  def setup_current_group_info(example_group)
    @current_group_info = GroupInfo.new( example_group )
    @current_file_info.groups << @current_group_info
  end
  
  def setup_current_example_info(example)
    @current_example_info = ExampleInfo.new( example )
    @current_group_info.examples << @current_example_info
  end
  
  def filename_for(example_group)
    example_group.location =~ /^(.*):([0-9])+/
    filename = $1
    lineno = $2
    filename = filename[ SPEC_DIR.length..-1 ]
    if ( filename[0,1] == "/" ) 
      filename = filename[1..-1] 
    end
    filename
  end
  
  def example_started(example)
    example_finished() unless ( @current_example.nil? )
    
    setup_current_example_info( example )
  end
  
  def example_finished()
  	@current_example_info.finish
  	@current_example_info = nil
  end
  
  def example_passed(example)
    @current_example_info.status = :passing
    example_finished
  end
  
  def example_failed(example, counter, failure)
    return unless @current_example_info
    @current_example_info.status = :failing
    @current_example_info.failure = failure
    example_finished
  end
  
  def example_pending(example, message)
    @current_example_info.status = :pending
    @current_example_info.failure = message
    example_finished
  end
  
  def xml_escape(str)
    str.gsub( /&/, '&amp;' ).gsub( /"/, '&quot;' )
  end
  
  def start_dump
    files.each do |f|
      output_filename = File.join( output, "TEST-#{File.basename(f.path, '.rb')}" ) + '.xml'
      FileUtils.mkdir_p( File.dirname( output_filename ) )
      File.open( output_filename, 'w' ) do |output|
        output.puts( %Q(<?xml version="1.0" encoding="UTF-8" ?>) )
        output.puts( %Q(<testsuite name="#{f.path}" time="#{f.duration}" tests="#{f.examples.size}" errors="#{f.failing.size}" skipped="#{f.pending.size}">) )
        f.groups.each do |g|
          g.examples.each do |ex|
            case_name = xml_escape( g.rspec_group.description + ' ' + ex.rspec_example.description )
            output.puts( %Q(  <testcase time="#{ex.duration}" name="#{case_name}">) )
            if ( ex.status == :pending )
              output.puts( %Q(    <skipped/>) )
            elsif ( ex.status == :failing )
              output.puts( %Q(    <failure/>) )
            end
            output.puts( %Q(  </testcase>) )
          end
        end
        output.puts( %Q(</testsuite>) )
      end
    end
  end
  
end
