module Maven
  module Tools
    class Tag

      def self.tags(*tags)
        if tags.size == 0
          @tags
        else
          #self.send :attr_accessor, *tags
          tags.each do |tag|
          eval <<-EOF
            def #{tag.to_s}(val = nil)
              @#{tag.to_s} = val if val
              @#{tag.to_s}
            end
            def #{tag.to_s}=(val)
              @#{tag.to_s} = val
            end
EOF
          end
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
            val.flatten!
            if val.size > 0
              buf << "#{indent}  <#{var}>\n"
              val.each do |v|
                if v.is_a? Tag
                  v.to_xml(buf, indent + "    ")
                else
                  buf << "#{indent}    <#{var.to_s.sub(/s$/, '')}>#{v}</#{var.to_s.sub(/s$/, '')}>\n"
                end
              end
              buf << "#{indent}  </#{var}>\n"
            end
          when Hash
            if val.size > 0
              buf << "#{indent}  <#{var}>\n"
              val.each do |k, v|
                if v.is_a? Tag
                  v.to_xml(buf, indent + "    ")
                else
                  buf << "#{indent}    <#{k}>#{v}</#{k}>\n"
                end
              end
              buf << "#{indent}  </#{var}>\n"
            end
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

      def ==(other)
        group_id == other.group_id && artifact_id == other.artifact_id
      end
    end

    class Dependency < Coordinate
      tags :type, :scope, :classifier
      def initialize(group_id, artifact_id, version = nil, type = nil)
        super(:group_id => group_id, :artifact_id => artifact_id, :version => version, :type => type)
      end
    end

    class Gem < Dependency
      tags :dummy
      def initialize(gemname, version = nil)
        super("rubygems", gemname, version, "gem")
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
        item = 
          case args
          when Dependency
            args
          when Array
            if args.size == 1
              args << "[0.0.0,)"
            elsif args.size == 2 && args.last.is_a?(Hash)
              args = [args[0], "[0.0.0,)", args[1]]
            elsif args.size >= 2
              if args[1] =~ /~>/
                val = args[1].sub(/.*\ /, '')
                last = val.sub(/\.[^.]+$/, '.99999')
                args[1] = "[#{val}, #{last}]"
              elsif args[1] =~ />=/
                val = args[1].sub(/.*\ /, '')
                args[1] = "[#{val},)"
              elsif args[1] =~ /<=/
                val = args[1].sub(/.*\ /, '')
                args[1] = "[0.0.0,#{val}]"
              elsif args[1] =~ />/
                val = args[1].sub(/.*\ /, '')
                args[1] = "(#{val},)"
              elsif args[1] =~ /</
                val = args[1].sub(/.*\ /, '')
                args[1] = "[0.0.0,#{val})"
              end
            end
            if args[0] =~ /:/
              Dependency.new(args[0].sub(/:[^:]+$/, ''), 
                             args[0].sub(/.*:/, ''), 
                             args[1])
            elsif args[0] =~ /\./
              Dependency.new(args[0].sub(/\.[^.]+$/, ''), 
                             args[0].sub(/.*\./, ''), 
                             args[1])
            else
              args = args.delete(2) if args[2]
              Gem.new(*args)
            end
          when String
            if args =~  /:/
              Dependency.new(args.sub(/:[^:]+$/, ''), 
                             args.sub(/.*:/, ''))
            elsif args =~  /\./
              Dependency.new(args.sub(/\.[^.]+$/, ''), 
                             args.sub(/.*\./, ''))
            else
              Gem.new(args)
            end
          else
            args
          end
        super item unless member? item
        item
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

      def to_xml(buf = "", indent = "")
        if @finalName.nil? && (@plugins.nil? || @plugins.size == 0)
          ""
        else
          super
        end
      end
    end

    class Plugin < Coordinate
      tags :extensions, :configuration, :executions
      def initialize(group_id, artifact_id, version, &block)
        super(:artifact_id => artifact_id, :version => version)
        @group_id = group_id if group_id
        if block
          block.call(self)
        end
        self
      end

      def with(config)
        self.configuration = config
      end

      def in_phase(phase, name = nil, &block)
        name = "in_phase_#{phase.gsub(/-/,'_')}" unless name
        self.executions.get(name) do |exe|
          exe.phase = phase
          block.call(exe) if block
          exe
        end
      end

      def executions
        @executions ||= ModelHash.new(Execution)
      end

      def execution(name = nil, &block)
        executions.get(name, &block)
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
      tags :id, :phase, :goals, :inherited, :configuration

      def initialize(id = nil)
        super({ :id => id })
      end

      def configuration=(c)
        if c.is_a? Hash
          @configuration = Configuration.new(c)
        else
          @configuration = c
        end
      end

      def execute_goal(g)
        self.goals = g
        self
      end

      def with(config)
        self.configuration = config
      end

      def goals
        @goals ||= []
      end

      def goals=(goals)
        @goals = goals.is_a?(Array) ? goals: [goals]
      end
    end

    class DependencyManagement < Tag
      tags :dependencies

      def _name
        "dependencyManagement"
      end

      def dependencies(&block)
        @dependencies ||= DepArray.new
        if block
          block.call(@dependencies)
        end
        @dependencies
      end
    end

    class Profile < Tag
      tags :id, :activation, :dependencies, :dependency_management, :properties, :build

      def initialize(id, &block)
        super(:id => id)
        if block
          block.call(self)
        end
        self
      end
      
      def properties
        (@properties || Properties.new).map
      end

      def properties=(props)
        if props.is_a? Hash
          @properties = Properties.new(props)
        else
          @properties = props
        end
      end

      def build
        @build ||= Build.new
      end
      
      def activation(name = nil, value = nil, &block)
        @activation ||= Activation.new
         if name || value
           warn "deprecated, use 'property_activation' instead"
           @activation.property(name, value)
         else
           block.call(@activation) if block
           @activation
         end
      end

      def default_activation(name = nil, value = nil)
        warn "deprecated, use 'activation.by_default.property(name.value)' instead"
        activation.property(name, value).by_default
      end

      def plugin(*args, &block)
        build.plugins.get(*args, &block)
      end

      def dependencies(&block)
        @dependencies ||= DepArray.new
        if block
          block.call(@dependencies)
        end
        @dependencies
      end

      def dependency_management(&block)
        @dependency_management ||= DependencyManagement.new
        if block
          block.call(@dependency_management.dependencies)
        end
        @dependency_management.dependencies
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
      
      def default_model
        @default_model ||= 
          begin
            model = @clazz.new
            self[nil] = model
            model
          end
      end

      def method_missing(method, *args, &block)
        default_model.send(method, *args, &block)
      end
    end
    
    class DeveloperHash < Hash

      def get(*args, &block)
        developer = if args.size == 1 && args[0].is_a?(Developer)
                      args[0] 
                    else 
                      Developer.new(*args)
                    end
        self[developer.id] = developer
        if block
          block.call(developer)
        end
        developer
      end
      alias :new :get
      alias :add :get
    end

    class LicenseHash < Hash

      def get(*args, &block)
        license = if args.size == 1 && args[0].is_a?(License)
                      args[0] 
                    else 
                      License.new(*args)
                    end
        self[license.name] = license
        if block
          block.call(license)
        end
        license
      end
      alias :new :get
      alias :add :get
    end

    class PluginHash < Hash

      def get(*args, &block)
        case args.size
        when 3
          name = args[0] + ":" + args[1]
          version = args[2]
        when 2
          name = args[0].to_s
          version = args[1]
        when 1
          name = args[0].to_s
        else
          raise "need name"
        end
        if (name =~ /\./).nil?
          if [:jruby, :gem, :rspec, :rake, :rails2, :rails3, :gemify, :cucumber, :runit].member? name.to_sym
            group_id = 'de.saumya.mojo'
            artifact_id = "#{name}-maven-plugin"
          else
            group_id = nil
            artifact_id = "maven-#{name}-plugin"
          end
        else
          group_id = name.sub(/:.+$/, '')
          artifact_id = name.sub(/^.+:/, '')
        end
        k = "#{group_id}:#{artifact_id}".to_sym
        result = self[k]
        if result.nil?
          result = (self[k] = Plugin.new(group_id, artifact_id, version))
        end
        result.version = version if version
        if block
          block.call(result)
        end
        result
      end
      alias :new :get
      alias :add :get
      
    end

    class Developer < Tag
      tags :id, :name, :email

      def initialize(*args)
        case args.size
        when 1
          @email = args[0].sub(/.*</, '').sub(/>.*/, '')
          @name = args[0].sub(/\s*<.*/, '')
        when 2
          @name = args[0]
          @email = args[1]
        when 3
          @id = args[0]
          @name = args[1]
          @email = args[2]
        end
        @id = @email.sub(/@/, '_at_').gsub(/\./, '_dot_') unless @id
        self
      end
    end

    class License < Tag
      tags :name, :url, :distribution

      def initialize(*args)
        case args.size
        when 1
          @url = args[0]
          @name = args[0].sub(/.*\//, '').sub(/\.\w+/, '')
        when 2
          @url = args[0]
          @name = args[1]
        when 3
          @url = args[0]
          @name = args[1]
          @distribution = args[2]
        end
        @url = "./#{url}" unless @url =~ /^\.\//
        @distribution = "repo" unless @distribution
        self
      end
    end

    class Project < Tag
      tags :model_version, :group_id, :artifact_id, :version, :name, :packaging, :description, :url, :developers, :licenses, :repositories, :plugin_repositories, :dependencies, :dependency_management, :properties, :build, :profiles

      def initialize(group_id, artifact_id, version = "0.0.0", &block)
        super(:model_version => "4.0.0", :artifact_id => artifact_id, :group_id => group_id, :version => version)
        if block
          block.call(self)
        end
        self
      end

      def _name
        "project"
      end

      def name(val = nil)
        @name = val if val
        @name
      end

      def name=(val)
        @name = "<![CDATA[#{val}]]>"
      end

      def description(val = nil)
        @description = val if val
        @description
      end

      def description=(val)
        @description = "<![CDATA[#{val}]]>"
      end

      def execute_in_phase(phase, name = nil, &block)
        plugin("gem").in_phase(phase.to_s, name).execute_goal("execute_in_phase").with(:file => current_file, :phase => phase)
        executions_in_phase[phase.to_s] = block
      end

      def executions_in_phase
        @executions_in_phase ||= {}
      end

      def plugin(*args, &block)
        build.plugins.get(*args, &block)
      end

      def profile(*args, &block)
        profiles.get(*args, &block)
      end

      def build
        @build ||= Build.new
      end

      def developers(&block)
        @developers ||= DeveloperHash.new
        if block
          block.call(@developers)
        end
        @developers
      end

      def licenses(&block)
        @licenses ||= LicenseHash.new
        if block
          block.call(@licenses)
        end
        @licenses
      end

      def repositories(&block)
        @repositories ||= ModelHash.new(Repository)
        if block
          block.call(@repositories)
        end
        @repositories
      end

      def repository(*args, &block)
        repositories.get(*args, &block)
      end

      def plugin_repositories(&block)
        @plugin_repositories ||= ModelHash.new(PluginRepository)
        if block
          block.call(@plugin_repositories)
        end
        @plugin_repositories
      end

      def plugin_repository(*args, &block)
        plugin_repositories.get(*args, &block)
      end

      def dependencies(&block)
        @dependencies ||= DepArray.new
        if block
          block.call(@dependencies)
        end
        @dependencies
      end

      def dependency_management(&block)
        @dependency_management ||= DependencyManagement.new
        if block
          block.call(@dependency_management.dependencies)
        end
        @dependency_management.dependencies
      end

      def properties
        @properties ||= Properties.new
        @properties.map
      end

      def properties=(props)
        if props.is_a? Hash
          @properties = Properties.new(props)
        else
          @properties = props
        end
      end

      def profiles(&block)
        @profiles ||= ModelHash.new(Profile)
        if block
          block.call(@profiles)
        end
        @profiles
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
        # sort the hash over the keys
        map.collect { |k,v| [k.to_s, v]}.sort.each do |k,v|
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

      def map
        @props
      end
    end

    class ListItems < Tag

      def initialize(name = nil)
        @name = name
      end

      def add(item)
        @items ||= Array.new
        @items << item
      end
      alias :<< :add

      def to_xml(buf = "", indent = "")
        buf << "#{indent}<#{@name}>\n" if @name 
        @items.each do |i|
          i.to_xml(buf, indent)
        end
        buf << "#{indent}</#{@name}>\n" if @name
      end
      
    end

    class OS < Tag
      tags :name, :family, :arch, :version
    end

    class Activation < Tag
      tags :activeByDefault, :property, :os
      def initialize
        super({})
      end

      def add_property(name, value)
        warn "deprecated, use 'property' instead"
        property(name, value)
      end

      def property(name, value)
        if name && value
          # TODO make more then one property
          raise "more then one property is not implemented: #{@property.name} => #{@property.value}" if @property
          @property ||= ListItems.new
          @property << Property.new(:name => name, :value => value)
        end
        self
      end

      def os(&block)
        @os ||= OS.new
        block.call(@os) if block
        @os
      end

      def as_default
        warn "deprecated, use 'by_default' instead"
        by_default
      end
      
      def by_default(value = true)
        @activeByDefault = value
        self
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
