# TODO make nice require after ruby-maven uses the same ruby files
require File.join(File.dirname(__FILE__), 'utils.rb')
require File.join(File.dirname(File.dirname(__FILE__)), 'tools', 'coordinate.rb')
#require 'maven/tools/coordinate'

module Maven
  module Model

    class DependencyArray < Array
      def <<(dep)
        raise "not of type Dependency" unless dep.is_a?(Dependency)
        d = detect { |item| item == dep }
        if d
          d.version = dep.version
          self
        else
          super
        end
      end
      alias :push :<<
    end

    class ExclusionArray < Array
      def <<(*dep)
        excl = dep[0].is_a?(Exclusion) ? dep[0]: Exclusion.new(*dep.flatten)
        delete_if { |item| item == excl }
        super excl
      end
      alias :push :<<
    end

    class Coordinate < Tag

      private

      include ::Maven::Tools::Coordinate

      public

      tags :group_id, :artifact_id, :version
      def initialize(*args)
        @group_id, @artifact_id, @version = gav(*args.flatten)
      end

      def version?
        !(@version.nil? || @version == '[0,)')
      end

      def hash
        "#{group_id}:#{artifact_id}".hash
      end

      def ==(other)
        group_id == other.group_id && artifact_id == other.artifact_id
      end
      alias :eql? :==

    end

    class Parent < Coordinate
      tags :relative_path

    end

    class Exclusion < Tag
      tags :group_id, :artifact_id
      def initialize(*args)
        @group_id, @artifact_id = group_artifact(*args)
      end

      def hash
        "#{group_id}:#{artifact_id}".hash
      end

      def ==(other)
        group_id == other.group_id && artifact_id == other.artifact_id
      end
      alias :eql? :==

      private
      
      include ::Maven::Tools::Coordinate
    end

    class Dependency < Coordinate
      tags :type, :scope, :classifier, :exclusions
      def initialize(type, *args)
        super(*args)
        @type = type
        args.flatten!
        if args[0] =~ /:/ && args.size == 3
          @classifier = args[2] unless args[2] =~ /[=~><]/
        elsif args.size == 4
          @classifier = args[3] unless args[3] =~ /[=~><]/
        end
      end

      def hash
        "#{group_id}:#{artifact_id}:#{@type}:#{@classifier}".hash
      end

      def ==(other)
        super && @type == other.instance_variable_get(:@type) && @classifier == other.instance_variable_get(:@classifier)
      end
      alias :eql? :==

      def self.new_gem(gemname, *args)
        new(:gem, "rubygems", gemname, *args)
      end

      def self.new_pom(*args)
        new(:pom, *args)
      end

      def self.new_jar(*args)
        new(:jar, *args)
      end

      def self.new_test_jar(*args)
        result = new(:jar, *args)
        result.scope :test
        result
      end

      def exclusions(&block)
        @exclusions ||= ExclusionArray.new
        if block
          block.call(@exclusions)
        end
        @exclusions
      end

      def exclude(*args)
        exclusions << args
      end
    end

    module Dependencies
    
      def self.included(parent)
        parent.tags :dependencies
      end

      def jar?(*args)
        dependencies.member?(Dependency.new(:jar, *args))
      end

      def test_jar?(*args)
        dependencies.member?(Dependency.new_test_jar(*args))
      end

      def gem?(*args)
        dependencies.member?(Dependency.new(:gem, ['rubygems', *args].flatten))
      end

      def detect_gem(name)
        dependencies.detect { |d| d.type.to_sym == :gem && d.artifact_id == name }
      end

      def pom?(*args)
        dependencies.member?(Dependency.new_pom(*args))
      end

      def dependencies(&block)
        @dependencies ||= DependencyArray.new
        if block
          block.call(self)
        end
        @dependencies
      end

      def add_dependency(dep, has_version = true, &block)
        d = dependencies.detect { |d| d == dep }
        if d
          if has_version
            d.version = dep.version
          end
          dep = d
        else
          dependencies << dep
        end
        block.call(dep) if block
        dep
      end
      private :add_dependency

      def add_gem(*args, &block)
        args = args.flatten
        if args.size == 1
          dep = Dependency.new_gem(*args)
          dep = dependencies.detect { |d| d == dep }
          if dep
            return dep
          end
          args[1] = ">= 0"
        end
        add_dependency(Dependency.new_gem(*args), &block)
      end
      private :add_gem

      def jar(*args, &block)
        if args.last.is_a?(Hash)
          raise "hash not allowed for jar"
        end
        add_dependency(Dependency.new_jar(args), args.size > 1, &block)
      end

      def test_jar(*args, &block)
        if args.last.is_a?(Hash)
          raise "hash not allowed for test_jar"
        end
        add_dependency(Dependency.new_test_jar(args), args.size > 1, &block)
      end
      
      def gem(*args, &block)
        if args.last.is_a?(Hash)
          raise "hash not allowed in that context"
        end
        add_gem(args, &block)
      end

      def pom(*args, &block)
        if args.last.is_a?(Hash)
          raise "hash not allowed in that context"
        end
        add_dependency(Dependency.new_pom(args), args.size > 1, &block)
      end
    end
  end
end
