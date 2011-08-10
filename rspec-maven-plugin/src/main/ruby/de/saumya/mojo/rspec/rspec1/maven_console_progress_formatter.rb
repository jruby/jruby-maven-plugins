
require 'spec/runner/formatter/base_formatter'

class MojoLog

  def info(str)
    puts str
  end  
  
end

MOJO_LOG = MojoLog.new

class MavenConsoleProgressFormatter < Spec::Runner::Formatter::BaseFormatter
  
  def initialize(options, where)
    super( options, where )
    @first = true
    @passing = []   
    @pending = []
    @failing = []   
    @filename = nil
  end
  
  def example_group_started(example_group)
    #MOJO_LOG.info( "spec dir #{SPEC_DIR}")
    #MOJO_LOG.info( "location #{example_group.location}" )
    unless ( @first )
      MOJO_LOG.info( "  #{@passing.size} passing; #{@failing.size} failing; #{@pending.size} pending")
    end
    @first = false
    @passing = []   
    @pending = []
    @failing = []   
    example_group.location =~ /^(.*):([0-9])+/
    filename = $1
    lineno = $2
    filename = filename[ SPEC_DIR.length..-1 ]
    if ( filename[0,1] == "/" ) 
      filename = filename[1..-1] 
    end
    unless ( @filename == filename )
      @filename = filename
      MOJO_LOG.info( "SPEC: #{@filename}" )
    end
    
    MOJO_LOG.info( "  - #{example_group.description}" )
    super( example_group )
  end
  
  def example_passed(example)
    @passing << example 
  end
  
  def example_pending(example, message)
    @pending << example 
  end
  
  def example_failed(example, counter, failure)
    @failing << example 
  end
  
  def dump_summary(duration, example_count, failure_count, pending_count)
    unless ( @first )
      MOJO_LOG.info( "  #{@passing.size} passing; #{@failing.size} failing; #{@pending.size} pending")
    end
    pass_count = example_count - ( failure_count + pending_count ) 
    MOJO_LOG.info( "=========================================" )
    MOJO_LOG.info( "TOTAL: #{pass_count} passing; #{failure_count} failing; #{pending_count} pending")

    if SUMMARY_REPORT

      # Creating the XML report
      FileUtils.mkdir_p File.dirname SUMMARY_REPORT
      content = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>
<testsuite failures=\"#{failure_count}\" time=\"#{duration}\" errors=\"0\" skipped=\"#{pending_count}\" tests=\"#{example_count}\" name=\"Ruby\">\n"

      # Passing
      @passing.each { |test|
        content = content + "<testcase time=\"0.0\" name=\"#{test.description}\"/>\n"
      }
	
      # Pending - considering as failures
      @pending.each { |test|
        content = content + "<testcase time=\"0.0\" name=\"#{test.description}\"><failure></failure></testcase>\n"
      }
	
      # Failures
      @failing.each { |test|
        content = content + "<testcase time=\"0.0\" name=\"#{test.description}\"><failure></failure></testcase>\n"
      }
	
      content = content + "</testsuite>"

      File.open(SUMMARY_REPORT, 'w+') {|f| f.write(content) }
    end
  end
  
end
