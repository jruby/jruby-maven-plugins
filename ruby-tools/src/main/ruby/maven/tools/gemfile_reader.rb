::MAVEN = "maven3"
module Maven
  module Tools
    class GemfileReader

      attr_reader :groups

      def initialize(file)
        gemfile = case file
                  when String
                    file 
                  when File
                    File.read(file)
                  else
                    raise "input must be either a File or a String. it is '#{file.class}'"
                  end
        eval "class ::#{self.class}\n#{gemfile}\nend"
      end

      def self.current
        @current ||= [[:default]]
      end

      def self.groups
        @groups ||= {:default => []}
      end

      def groups
        self.class.groups
      end

      def self.gem(*args)
        if(args[0] =~ /[^.]+\.[^.]+/)
          jar(*args)
        else
          current.last.each do |c|
            g = (groups[c] ||= [])
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
        raise "name and version must be given" if args.size == 1 || (args.size == 2 && args[1].is_a?(Hash))
        current.last.each do |c|
          g = (groups[c] ||= [])
          def args.type
            :jar
          end
          g << args
        end
      end

      # def self.group(*args, &block)
      #   current << args
      #   block.call if block
      #   current.pop
      # end

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
    end
  end
end
