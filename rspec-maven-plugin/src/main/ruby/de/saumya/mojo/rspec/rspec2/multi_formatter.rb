
require 'rspec/core/formatters/base_formatter'

class MultiFormatter 

  def self.formatters
    @formatters ||= []
  end
  
  def initialize(output)
    @formatters = []
    MultiFormatter.formatters.each do |formatter_setup|
      formatter_class, formatter_output = *formatter_setup
      @formatters << formatter_class.new( formatter_output || output )
    end
  end
  
  def method_missing(sym, *args)
    @formatters.each do |formatter|
      formatter.send( sym, *args ) if formatter.respond_to? sym
    end
  end

  def respond_to? method
    true
  end
  
end
