require File.join(File.dirname(__FILE__), 'model_utils.rb')

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
      tags :group_id, :artifact_id, :version
      def initialize(*args)
        @group_id, @artifact_id, @version = *coordinate(*args.flatten)
      end

      def hash
        "#{group_id}:#{artifact_id}".hash
      end

      def ==(other)
        group_id == other.group_id && artifact_id == other.artifact_id
      end
      alias :eql? :==

      private

      def coordinate(*args)
        if args[0] =~ /:/
          [args[0].sub(/:[^:]+$/, ''), args[0].sub(/.*:/, ''), convert_version(*args[1, 2])]
        else
          [args[0], args[1], convert_version(*args[2,3])]
        end
      end

      def convert_version(*args)
        if args.size == 0
          nil
        else
          low, high = convert(args[0])
          low, high = convert(args[1], low, high) if args[1]
          if low == high
            low
          else
            "#{low || '[0.0.0'},#{high || ')'}"
          end
        end
      end

      def convert(arg, low = nil, high = nil)
        if arg =~ /~>/
          val = arg.sub(/~>\s*/, '')
          last = val.sub(/\.[^.]+$/, '.99999')
          ["[#{val}", "#{last}]"]
        elsif arg =~ />=/
          val = arg.sub(/>=\s*/, '')
          ["[#{val}", (nil || high)]
        elsif arg =~ /<=/
          val = arg.sub(/<=\s*/, '')
          [(nil || low), "#{val}]"]
        elsif arg =~ />/
          val = arg.sub(/>\s*/, '')
          ["(#{val}", (nil || high)]
        elsif arg =~ /</
          val = arg.sub(/<\s*/, '')
          [(nil || low), "#{val})"]
        elsif arg =~ /=/
          val = arg.sub(/=\s*/, '')
          [val, val]
        else
          [arg, arg]
        end
      end
    end

    class Exclusion < Tag
      tags :group_id, :artifact_id
      def initialize(*args)
        @group_id, @artifact_id = *coordinate(*args)
      end

      def hash
        "#{group_id}:#{artifact_id}".hash
      end

      def ==(other)
        group_id == other.group_id && artifact_id == other.artifact_id
      end
      alias :eql? :==

      private
      
      def coordinate(*args)
        case args.size
        when 1
          name = args[0].sub(/^mvn:/, '')
          if name =~ /:/
            [name.sub(/:[^:]+$/, ''), name.sub(/.*:/, '')]
          else
            ["rubygems", name]
          end
        else
          [args[0], args[1]]
        end
      end
    end

    class Dependency < Coordinate
      tags :type, :scope, :classifier, :exclusions
      def initialize(type, *args)
        super(*args)
        @type = type
      end

      def hash
        "#{group_id}:#{artifact_id}:#{@type}".hash
      end

      def ==(other)
        super && @type == other.instance_variable_get(:@type)
      end
      alias :eql? :==

      def self.new_gem(gemname, *args)
        if gemname =~ /^mvn:/
          new(:maven_gem, gemname.sub(/^mvn:/, ''), *args)
        else
          new(:gem, "rubygems", gemname, *args)
        end
      end

      def self.new_maven_gem(gemname, *args)
        new(:maven_gem, gemname.sub(/^mvn:/, ''), *args)
      end

      def self.new_jar(*args)
        new(:jar, *args)
      end

      def self.new_test_jar(*args)
        new(:test_jar, *args)
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
        dependencies.member?(Dependency.new(:test_jar, *args))
      end

      def gem?(*args)
        dependencies.member?(Dependency.new(:gem, ['rubygems', *args].flatten))
      end

      def maven_gem?(*args)
        dependencies.member?(Dependency.new_maven_gem(*args))
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
          args[1] = ">= 0.0.0"
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

      def maven_gem(*args, &block)
        if args.last.is_a?(Hash)
          raise "hash not allowed in that context"
        end
        add_dependency(Dependency.new_maven_gem(args), args.size > 1, &block)
      end
    end
  end
end
