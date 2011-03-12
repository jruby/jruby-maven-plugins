require 'rspec/core/metadata'

module RSpec
  module Core
    class Metadata < Hash

      module LocationKeys
        def first_caller_from_outside_rspec
          #puts self[:caller].inspect
          self[:caller].detect {|l| l !~ /\/lib\/rspec\/core/ && l !~ /AbstractScript.java/ }
        end
      end
    end
  end
end