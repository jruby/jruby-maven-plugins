gem 'minitest'
require 'minitest/autorun'
require 'jars/setup'

describe 'something' do

  it 'uses the right minitest version' do
    Gem.loaded_specs['minitest'].version.to_s.must_equal '5.7.0'
  end

  it 'runs from the jar' do
    __FILE__.must_equal 'test.rb'
    Dir.pwd.must_equal 'uri:classloader://WEB-INF/classes/'
  end

  it 'can use logger' do
    old = java.lang.System.err
    bytes = StringIO.new
    java.lang.System.err = java.io.PrintStream.new( bytes.to_outputstream )
    begin

      # TODO capture java stderr and ensure there is no warning
      org.slf4j.LoggerFactory.get_logger('me').info 'hello'

      java.lang.System.err.flush

      bytes.string.strip.must_equal '[main] INFO me - hello'

    ensure
      java.lang.System.err = old
    end

  end

end
