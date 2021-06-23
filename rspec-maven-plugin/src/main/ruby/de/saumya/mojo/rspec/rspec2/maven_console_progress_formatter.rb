require 'rspec/core/formatters/base_formatter'

class MavenConsoleProgressFormatter < RSpec::Core::Formatters::BaseFormatter

  def initialize(output)
    super( output )
    @printed_stacks = {}
    @started_at = Time.now
  end

  def current_file
    @current_file
  end

  def current_line
    @current_line
  end

  def set_location(hash)
    set_current_file( hash[:file_path] )
    set_current_line( hash[:line_number] )
  end

  def set_current_file(path)
    @current_file = relative_path( path )
  end

  def set_current_line(line_number)
    @current_line = line_number
  end

  def relative_path(path)
    if Pathname.new( path ).relative? && Pathname.new( BASE_DIR ).absolute?
      File.join( BASE_DIR, path )
    else
      Pathname.new( path ).relative_path_from( Pathname.new( BASE_DIR ) )
    end
  end


  def spec_file_started(spec_file)
    spec_file.metadata[:started_at] = Time.now
    meta = spec_file.metadata[:example_group]
    set_location( meta )

    @file_passing = []
    @file_failing = []
    @file_pending = []

    puts "** #{current_file}"
  end

  def example_group_started(example_group)
    super
    if ( example_group.top_level? )
      file_path = example_group.metadata[:example_group][:file_path]
      file_path = relative_path( file_path )
      if ( current_file != file_path )
        spec_file_started( example_group )
      end
    end
  end

  def example_started(example)
    super
    example.metadata[:spec_file_path] = current_file
    set_location( example.metadata )
    node = example.metadata[:example_group]
    depth = print_stack(node) + 1
    print "#{"  " * depth}#{example.metadata[:description]}"
  end

  def example_passed(example)
    super
    @file_passing << example
    example_completed(example)
  end


  def example_failed(example)
    super
    @file_failing << example
    example_completed(example, :failed)
  end

  def example_pending(example)
    super
    @file_pending << example
    example_completed(example, :pending)
  end

  def example_completed(example, status=nil)
    elapsed = Time.now - example.metadata[:execution_result][:started_at]
    print " - #{elapsed}s"

    if ( status )
      print " #{status.to_s.upcase}"
    end

    puts ""

  end

  def print_stack(node)
    depth = 1
    if ( node[:example_group] )
      depth = depth + print_stack( node[:example_group] )
    end
    puts "#{"  " * depth }#{node[:description]}" unless @printed_stacks[node[:description]]
    @printed_stacks[node[:description]] = true
    depth
  end

  def example_finished(example)
    super
  end

  def example_group_finished(example_group)
    super
  end

=begin
  def spec_file_finished(spec_file)
    elapsed = Time.now - spec_file.metadata[:started_at]
    puts ">> #{current_file} : #{elapsed}s : #{@file_passing.size} passing, #{@file_pending.size} pending, #{@file_failing.size} failing"
  end
=end

  # Dump

  def start_dump
  end

  def dump_failures()
    super
    return if ( failed_examples.empty? )
    puts "------------------------------------------------------------------------"
    puts "Failures"
    dump_example_list( failed_examples )
  end

  def dump_pending()
    super
    return if ( pending_examples.empty? )
    puts "------------------------------------------------------------------------"
    puts "Pending"
    dump_example_list( pending_examples )
  end

  def dump_example_list(examples)
    spec_file_path = nil
    examples.each do |example|
      if ( example.metadata[:spec_file_path] != spec_file_path )
        spec_file_path = example.metadata[:spec_file_path]
        puts "  #{spec_file_path}"
      end
      puts "    #{example.metadata[:line_number]}:#{example.metadata[:full_description]}"
    end
  end

  def dump_summary(duration, example_count, failure_count, pending_count)
    super
    elapsed = Time.now - @started_at
    passing_count = example_count - ( failure_count + pending_count )
    puts "------------------------------------------------------------------------"
    puts "Completed in #{elapsed}s #{failure_count > 0 ? 'WITH FAILURES' : ''}"
    puts "------------------------------------------------------------------------"
    puts "Passing...#{passing_count}"
    puts "Pending...#{pending_count}"
    puts "Failing...#{failure_count}"
    puts ""
    puts "Total.....#{example_count}"
    puts "------------------------------------------------------------------------"
  end

end
