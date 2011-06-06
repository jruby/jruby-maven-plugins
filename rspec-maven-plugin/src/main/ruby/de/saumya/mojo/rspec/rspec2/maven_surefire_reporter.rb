
require 'rspec/core/formatters/base_formatter'
require 'rspec/core/formatters/snippet_extractor'
require 'fileutils'

class MavenSurefireReporter < RSpec::Core::Formatters::BaseFormatter

  attr_reader :passing_examples

  def initialize(output)
    super( output )
    @started_at = Time.now
    @passing_examples = []
  end
  
  def example_passed(example)
    super
    @passing_examples << example
  end
  
  def dump_summary(duration, example_count, failure_count, pending_count)
    elapsed = Time.now - @started_at
    reporter = self
    extractor = SnippetExtractor.new
    report_file = File.join( TARGET_DIR, 'surefire-reports', 'TEST-rspec.xml' )
    FileUtils.mkdir_p( File.dirname( report_file ) )
    File.open( report_file, 'w' ) do |report_io|
      Emitter.new( report_io ) do
        tag( :testsuite, :time=>elapsed, :errors=>0, :tests=>example_count, :skipped=>pending_count, :failures=>failure_count, :name=>File.basename( BASE_DIR ) ) do
          reporter.passing_examples.each do |ex|
            class_name = File.basename( ex.metadata[:file_path], '_spec.rb' )
            tag( :testcase, :time=>ex.metadata[:execution_result][:run_time], :classname=>class_name, :name=>ex.metadata[:description] )
          end
          reporter.pending_examples.each do |ex|
            class_name = File.basename( ex.metadata[:file_path], '_spec.rb' )
            tag( :testcase, :time=>ex.metadata[:elapsed], :classname=>class_name, :name=>ex.metadata[:description] ) do
              tag( :skipped )
            end
          end
          reporter.failed_examples.each do |ex|
            class_name = File.basename( ex.metadata[:file_path], '_spec.rb' )
            exception = ex.metadata[:execution_result][:exception]
            tag( :testcase, :time=>ex.metadata[:execution_result][:run_time], :classname=>class_name, :name=>ex.metadata[:description] ) do
            relevant_line = reporter.find_relevant_line( ex, exception )
              tag( :failure, :message=>relevant_line ) do
                content( exception.backtrace() )
              end
            end
          end
        end
      end
    end
  end
  
  def find_relevant_line(example, exception)
    match_prefix = example.metadata[:file_path].to_s
    file_line = nil
    exception.backtrace.each do |stack_line|
      if ( stack_line.to_s[0, match_prefix.length] == match_prefix )
        file_line = stack_line
        break
      end
    end
    if ( file_line )
      file, line, junk = file_line.split( ':' )
      if ( File.exist?( file ) ) 
        lines = File.readlines( file )
        return lines[line.to_i-1].strip
      end
    end
    nil
  end
  
  
  class Converter
    def convert(code,pre)
      code
    end
  end
  
  class Emitter
    def initialize(io, &block)
      @io = io
      @indent = 0
      @tag_stack = []
      decl
      instance_eval &block if block
    end
    
    def decl
      @io.puts( %q(<?xml version="1.0" encoding="UTF-8" ?>) )
    end
    
    def tag(tag_name, attrs={}, &block)
      if ( block ) 
        start_tag( tag_name, attrs )
        instance_eval &block if block
        end_tag()
      else
        start_tag( tag_name, attrs, false )
      end
    end
    
    def start_tag(tag_name, attrs={}, has_body=true)
      if ( has_body ) 
        @tag_stack.push( tag_name )
        @io.puts( indent + "<#{tag_name}#{attributes(attrs)}>" )
        @indent = @indent + 1
      else 
        @io.puts( indent + "<#{tag_name}#{attributes(attrs)}/>" )
      end
    end
    
    def content(str)
      str.each do |line|
        @io.puts( indent + escape( line.strip ) )
      end
    end
  
    def end_tag()
      @indent = @indent - 1
      @io.puts( indent + "</#{@tag_stack.pop}>" )
    end
    
    def attributes(attrs)
      str = attrs.entries.map{|e| e.first.to_s + "=" + quote(e.last.to_s) }.join( ' ' )
      str.strip!
      return '' if str.empty?
      " #{str}"
    end
    
    def quote(str)
      %q(") + escape( str ) + %q(")
    end
    
    def escape(str)
      str.gsub( /&/, '&amp;' ).gsub( /"/, '&quot;' ).gsub( /</, "&lt;" ).gsub( />/, '&gt;' )
    end
  
    def indent()
      "    " * @indent
    end 
  end
  
  class SnippetExtractor 

    def snippet_for(error_line)
      if error_line =~ /(.*):(\d+)/
        file = $1
        line = $2.to_i
        [lines_around(file, line), line]
      else
        ["# Couldn't get snippet for #{error_line}", 1]
      end
    end
  
    def lines_around(file, line)
      if File.file?(file)
        lines = File.open(file).read.split("\n")
        min = [0, line-3].max
        max = [line+1, lines.length-1].min
        selected_lines = []
        selected_lines.join("\n")
        lines[min..max].join("\n")
      else
        "# Couldn't get snippet for #{file}"
      end
    end
  end
end
