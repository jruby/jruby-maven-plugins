module Maven
  module Tools
    class Tag

      def self.tags(*tags)
        if tags.size == 0
          @tags
        else
          attr_accessor *tags
          @tags ||= []
          if self.superclass.respond_to?:tags
            @tags = self.superclass.tags || []
          else
            @tags ||= []
          end
          @tags.replace([ @tags, tags].flatten)
        end
      end

      def _name
        self.class.to_s.downcase.sub(/.*::/, '')
      end

      def initialize(args = {})
        args.each do |k,v|
          send("#{k}=".to_sym, v)
        end
      end

      def to_xml(buf = "", indent = "")
        buf << "#{indent}<#{_name}>\n"
        self.class.tags.each do |var|
          val = instance_variable_get("@#{var}".to_sym)
          var = var.to_s.gsub(/_(.)/) { $1.upcase }
          case val
          when Array
            buf << "#{indent}  <#{var}>\n"
            val.each do |v|
              if v.is_a? Tag
                v.to_xml(buf, indent + "    ")
              else
                buf << "#{indent}    <#{var.to_s.sub(/s$/, '')}>#{val}</#{var.to_s.sub(/s$/, '')}>\n"
              end
            end
            buf << "#{indent}  </#{var}>\n"
          when Hash
            buf << "#{indent}  <#{var}>\n"
            val.each do |k, v|
              if v.is_a? Tag
                v.to_xml(buf, indent + "    ")
              else
                buf << "#{indent}    <#{k}>#{v}</#{k}>\n"
              end
            end
            buf << "#{indent}  </#{var}>\n"
          when Tag
            val.to_xml(buf, indent + "  ")
          else
            #when String
            buf << "#{indent}  <#{var}>#{val}</#{var}>\n" if val
          end
        end
        buf << "#{indent}</#{_name}>\n"
        buf
      end
    end

    class Coordinate < Tag
      tags :group_id, :artifact_id, :version
      def initialize(args = {})
        super
      end
    end
    class Dependency < Coordinate
      tags :scope, :type
      def initialize(group_id, artifact_id, version, type = nil)
        super(:group_id => group_id, :artifact_id => artifact_id, :version => version, :type => type)
      end
    end

    class Gem < Dependency
      tags :dummy
      def initialize(*args)
        super("rubygems", args[0], args[1], "gem")
      end

      def _name
        "dependency"
      end
    end

    class NamedArray < Array
      attr_reader :name
      def initialize(name, &block)
        @name = name.to_s
        if block
          block.call(self)
        end
        self
      end
    end

    class DepArray < Array
      def <<(args)
        case args
        when Array
          if args.size == 1
            args << "[0.0.0,)"
          elsif args.size == 2 && args.last.is_a?(Hash)
            args = [args[0], "[0.0.0,)", args[1]]
          end
          if args[0] =~ /\./
            super Dependency.new(args[0].sub(/\.[^.]+$/, ''), 
                                 args[0].sub(/.*\./, ''), 
                                 args[1])
          else
            super Gem.new(*args)
          end
        when Hash
          super Dependency.new(args)
        else
          super args
        end
      end
    end

    class Build < Tag
      tags :finalName, :plugins
      def plugins(&block)
        @plugins ||= PluginHash.new
        if block
          block.call(@plugins)
        end
        @plugins
      end
    end

    class Plugin < Coordinate
      tags :extensions, :configuration, :executions
      def initialize(group_id, artifact_id, version, extensions = nil, &block)
        super(:artifact_id => artifact_id, :version => version)
        @group_id = group_id if group_id
        @extensions = true if extensions == true
        if block
          block.call(self)
        end
        self
      end

      def executions
        @executions ||= ModelHash.new(Execution)
      end

      def configuration=(c)
        if c.is_a? Hash
          @configuration = Configuration.new(c)
        else
          @configuration = c
        end
      end
    end

    class Execution < Tag
      tags :id, :phase, :goals, :configuration

      def initialize(id)
        super({ :id => id })
      end

      def configuration=(c)
        if c.is_a? Hash
          @configuration = Configuration.new(c)
        else
          @configuration = c
        end
      end
      def goals
        @goals ||= []
      end
    end

    class Profile < Tag
      tags :id, :activation, :dependencies, :properties, :build

      def initialize(id, &block)
        super(:id => id)
        if block
          block.call(self)
        end
        self
      end
      
      def properties=(p)
        if p.is_a? Hash
          @properties = Properties.new(p)
        else
          @properties = p
        end
      end

      def build
        @build ||= Build.new
      end
      
      def activation(name = nil, value = nil)
        @activation ||= Activation.new
        @activation.add_property(name, value)
        @activation
      end

      def default_activation(name = nil, value = nil)
        activation(name, value).as_default
      end

      def dependencies(&block)
        @dependencies ||= DepArray.new
        if block
          block.call(@dependencies)
        end
        @dependencies
      end
    end

    class ModelHash < Hash

      def initialize(clazz)
        @clazz = clazz
      end

      def get(key, &block)
        key = key.to_sym if key
        result = self[key]
        if result.nil?
          result = (self[key] = @clazz.new(key))
        end
        if block
          block.call(result)
        end
        result
      end
      alias :new :get
      
    end

    class PluginHash < Hash

      def get(*args, &block)
        case args.size
        when 3
          name = args[0] + "." + args[1]
          version = args[2]
        when 2
          name = args[0].to_s
          version = args[1]
        else
          raise "need name and version"
        end
        if (name =~ /\./).nil?
          if [:jruby, :gem, :rspec, :rake, :rails2, :rails3, :gemify].member? name.to_sym
            group_id = 'de.saumya.mojo'
            artifact_id = "#{name}-maven-plugin"
          else
            group_id = nil
            artifact_id = "maven-#{name}-plugin"
          end
        else
          group_id = name.sub(/\.[^.]+$/, '')
          artifact_id = name.sub(/^.+\./, '')
        end
        k = "#{group_id}:#{artifact_id}".to_sym
        result = self[k]
        if result.nil?
          result = (self[k] = Plugin.new(group_id, artifact_id, version))
        end
        result.version = version #if version
        if block
          block.call(result)
        end
        result
      end
      alias :new :get
      alias :add :get
      
    end

    class Project < Tag
      tags :model_version, :group_id, :artifact_id, :version, :name, :packaging, :repositories, :plugin_repositories, :dependencies, :properties, :build, :profiles

      def initialize(group_id, artifact_id, version = "0.0.0", &block)
        super(:model_version => "4.0.0", :artifact_id => artifact_id, :group_id => group_id, :version => version)
        if block
          block.call(self)
        end
        self
      end

      def build
        @build ||= Build.new
      end

      def repositories(&block)
        @repositories ||= ModelHash.new(Repository)
        if block
          block.call(@repositories)
        end
        @repositories
      end

      def plugin_repositories(&block)
        @plugin_repositories ||= ModelHash.new(PluginRepository)
        if block
          block.call(@plugin_repositories)
        end
        @plugin_repositories
      end

      def dependencies(&block)
        @dependencies ||= DepArray.new
        if block
          block.call(@dependencies)
        end
        @dependencies
      end

      def properties=(p)
        if p.is_a? Hash
          @properties = Properties.new(p)
        else
          @properties = p
        end
      end

      def profiles
        @profiles ||= ModelHash.new(Profile)
      end
    end

    class GemProject < Project
      tags :dummy
      def initialize(artifact_id, version = "0.0.0", &block)
        super("rubygems", artifact_id, version, &block)
      end
      def _name
        "project"
      end
    end

    class HashTag < Tag

      def initialize(name, args = {})
        @name = name
        @props = args
      end

      def [](key, value)
        @props ||= {}
        @props[key] = value
      end
      
      def to_xml(buf = "", indent = "")
        buf << "#{indent}<#{@name}>\n"
        map_to_xml(buf, indent, @props)
        buf << "#{indent}</#{@name}>\n"
      end
      
      def map_to_xml(buf, indent, map)
        map.each do |k,v|
          case v
          when Hash
            buf << "#{indent}  <#{k}>\n"
            map_to_xml(buf, indent + "  ", v)
            buf << "#{indent}  </#{k}>\n"
          when NamedArray
            buf << "#{indent}  <#{k}>\n"
            v.each do|i|
              buf << "#{indent}    <#{v.name}>\n"
              case i
              when Hash
                map_to_xml(buf, indent + "    ", i)
              end
              buf << "#{indent}    </#{v.name}>\n"
            end
            buf << "#{indent}  </#{k}>\n"
          when Array
            buf << "#{indent}  <#{k}>\n"
            singular = k.to_s.sub(/s$/, '')
            v.each do |i|
              buf << "#{indent}    <#{singular}>#{i}</#{singular}>\n"
            end
            buf << "#{indent}  </#{k}>\n"
          when /\n$/
            buf << "#{indent}  <#{k}>#{v}"
            buf << "#{indent}  </#{k}>\n"
          else
            buf << "#{indent}  <#{k}>#{v}</#{k}>\n"
          end
        end
      end
    end

    class Configuration < HashTag

      def initialize(args = {})
        super("configuration", args)
      end
    end

    class Properties < HashTag

      def initialize(args = {})
        super("properties", args)
      end
    end

    class Activation < Tag
      tags :activeByDefault, :property
      def initialize(name = nil, value = nil)
        super({})
        add_property(name, value) if name && value
      end

      def add_property(name, value)
        if name && value
          # TODO make more then one property
          raise "more then one property is not implemented: #{@property.name} => #{@property.value}" if @property
          @property = Property.new(:name => name, :value => value)
        end
      end

      def as_default
        @activeByDefault = true
      end
    end

    class Property < Tag
      tags :name, :value
    end

    class Repository < Tag
      tags :id, :url, :releases, :snapshots
      def initialize(id, &block)
        super({:id => id, :url => url})
        if block
          block.call(self)
        end
        self
      end

      def releases(args = {})
        @releases ||= args
      end

      def snapshots(args = {})
        @snapshots ||= args
      end
    end

    class PluginRepository < Repository
      tags :dummy
      def _name
        "pluginRepository"
      end
    end
  end
end
