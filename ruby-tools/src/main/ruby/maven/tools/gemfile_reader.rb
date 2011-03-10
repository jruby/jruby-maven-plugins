::MAVEN = "maven3"
module Maven
  module Tools
    class GemfileReader

      class Group < Array

        def properties(args = nil)
          if args
            @properties ||= {}
            @properties.merge!(args)
          else
            @properties ||= {}
          end
        end
      end

      attr_reader :groups

      def initialize(file)
        warn "deprecated, use gem_project instead"
        gemfile = case file
                  when String
                    file 
                  when File
                    File.read(file.path)
                  else
                    raise "input must be either a File or a String. it is '#{file.class}'"
                  end
        eval "class ::#{self.class}\n#{gemfile}\nend"
      end

      def self.current
        @current ||= [[:default]]
      end

      def self.groups
        @groups ||= {:default => Group.new}
      end

      def self.in_phase(name, &block)
        warn "in_phase is deprecated, use it inside 'Mavenfile'"
        blocks = phases[(name || '-dummy-').to_sym] ||= []
        blocks << block
      end

      def self.gem(*args)
        if(args[0] =~ /[^.]+\.[^.]+/)
          jar(*args)
        else
          current.last.each do |c|
            g = (groups[c] ||= Group.new)
            if args.last.is_a? Hash
              raise "git gems not supported" unless args.last[:git].nil?
            end
            def args.type
              :gem
            end
            g << args
          end
        end
      end

      def self.jar(*args)
        warn "in_phase is deprecated, use it inside 'maven.rb'"
        raise "name and version must be given: #{args.inspect}" if args.size == 1 || (args.size == 2 && args[1].is_a?(Hash))
        current.last.each do |c|
          g = (groups[c] ||= Group.new)
          def args.type
            :jar
          end
          g << args
        end
      end

      def self.plugin(*args, &block)
        warn "in_phase is deprecated, use it inside 'maven.rb'"
        raise "name and version must be given" if args.size == 1 || (args.size == 2 && args[1].is_a?(Hash))
        current.last.each do |c|
          g = (groups[c] ||= Group.new)
          def args.type
            :plugin
          end
          args << block if block
          g << args
        end
      end

      def self.properties(args = {})
        warn "in_phase is deprecated, use it inside 'maven.rb'"
        current.last.each do |c|
          g = (groups[c] ||= Group.new)
          g.properties(args)
        end
      end

      def self.method_missing(method, *args, &block)
        case method
        when :source
          raise "unsupported source" if args[0] != "http://rubygems.org"
        when :group
          current << args
          block.call if block
          current.pop
        end
      end

      def phases
        self.class.phases
      end

      def groups
        self.class.groups
      end

      def group(name)
        self.class.groups[(name || '-dummy-').to_sym] ||= Group.new
      end

      def execute_phase(name, project = nil)
        warn "execute_phase is deprecated, use 'execute_in_phase.rb'"
        (self.class.phases[(name || '-dummy-').to_sym] || []).each do |b|
          b.call(project)
        end
      end

      private
      def self.phases
        @phases ||= {}
      end
    end
  end
end

if $0 == __FILE__
  gemfile = Maven::Tools::GemfileReader.new(File.new(ARGV[0] || 'Gemfile'))
  gemfile.execute_phase(ARGV[1]) if ARGV[1]
end
