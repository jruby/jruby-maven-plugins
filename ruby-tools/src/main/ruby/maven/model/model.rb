require 'maven/model/dependencies'

module Maven
  module Model

    class Build < Tag
      tags :source_directory, :script_source_directory, :test_source_directory, :output_directory, :test_output_directory, :default_goal, :directory, :final_name, :plugins

      def plugins(&block)
        @plugins ||= PluginHash.new
        if block
          block.call(@plugins)
        end
        @plugins
      end

      def plugin?(name)
        plugins.key?(name)
      end

      def to_xml(buf = "", indent = "")
        if @final_name.nil? && (@plugins.nil? || @plugins.size == 0)
          ""
        else
          super
        end
      end
    end

    class Plugin < Coordinate
      tags :extensions, :configuration, :executions

      include Dependencies

      def initialize(*args, &block)
        super(*args)
        raise "plugin version must be a concrete version" if version =~ /^[\[\(].+,.*[\]\)]$/
        if block
          block.call(self)
        end
        self
      end

      def with(config)
        self.configuration config
      end

      def in_phase(phase, name = nil, &block)
        name = "in_phase_#{phase.to_s.gsub(/-/,'_')}" unless name
        self.executions.get(name) do |exe|
          exe.phase = phase
          block.call(exe) if block
          exe
        end
      end

      def execute_goal(goal)
        execution.execute_goal(goal)
      end

      def executions
        @executions ||= ModelHash.new(Execution)
      end

      def execution(name = nil, &block)
        executions.get(name, &block)
      end

      def configuration(c = nil)
        @configuration = Configuration.new(c) if c
        @configuration ||= Configuration.new({})
      end
    end

    class Execution < Tag
      tags :id, :phase, :goals, :inherited, :configuration

      def initialize(id = nil)
        self.id id if id
      end

      def configuration(c = nil)
        @configuration = Configuration.new(c) if c
        @configuration ||= Configuration.new({})
      end

      def execute_goal(g)
        self.goals = g
        self
      end

      def with(config)
        self.configuration config
      end

      def goals
        @goals ||= []
      end

      def goals=(goals)
        @goals = goals.is_a?(Array) ? goals: [goals]
      end
    end

    class DependencyManagement < Tag

      include Dependencies

      def _name
        "dependencyManagement"
      end
    end

    class Profile < Tag
      tags :id, :activation, :repositories, :plugin_repositories
      
      include Dependencies

      tags :dependency_management, :properties, :build

      def initialize(id, &block)
        self.id id
        if block
          block.call(self)
        end
        self
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

      def repositories(&block)
        @repositories ||= ModelHash.new(Repository)
        if block
          block.call(@repositories)
        end
        @repositories
      end

      def repository(id, url = nil, &block)
        repo = repositories.get(id, &block)
        repo.url = url if url
        repo
      end

      def plugin_repositories(&block)
        @plugin_repositories ||= ModelHash.new(PluginRepository)
        if block
          block.call(@plugin_repositories)
        end
        @plugin_repositories
      end

      def plugin_repository(id, url = nil, &block)
        repo = plugin_repositories.get(id, &block)
        repo.url = url if url
        repo
      end

      def plugin(*args, &block)
        build.plugins.get(*args, &block)
      end

      def dependency_management(&block)
        @dependency_management ||= DependencyManagement.new
        if block
          block.call(@dependency_management)
        end
        @dependency_management
      end
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
        @email = @email[0] if @email.is_a? Array # this produces a partial list
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
        @url = "./#{url}" unless @url =~ /^\.\// || @url =~ /^https?:\/\//
        @distribution = "repo" unless @distribution
        self
      end
    end


    class Project < Coordinate
      prepend_tags :model_version, :parent

      tags :name, :packaging, :description, :url, :developers, :licenses, :repositories, :plugin_repositories

      include Dependencies

      tags :dependency_management, :properties, :build, :profiles

      def initialize(*args, &block)
        super(*args)
        model_version "4.0.0"
        if block
          block.call(self)
        end
        self
      end
 
      def _name
        'project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"'
      end

      def version(val = nil)
        self.version = val if val
        @version ||= (@parent.nil? ? '0.0.0' : @parent.version)
      end

      def name(val = nil)
        self.name = val if val
        @name
      end

      def name=(val)
        @name = "<![CDATA[#{val}]]>"
      end

      def description(val = nil)
        self.description = val if val
        @description
      end

      def description=(val)
        @description = "<![CDATA[#{val}]]>"
      end
      
      def parent(*args, &block)
        @parent ||= Parent.new(*args)
        @parent.call(block) if block
        @parent
      end

      def execute_in_phase(phase, name = nil, &block)
        gem_plugin = plugin("gem")
        gem_plugin.in_phase(phase.to_s, name).execute_goal("execute_in_phase").with(:file => File.basename(current_file), :phase => phase)
        executions_in_phase[phase.to_s] = block
        gem_plugin
      end

      def executions_in_phase
        @executions_in_phase ||= {}
      end

      def plugin(*args, &block)
        build.plugins.get(*args, &block)
      end

      def plugin?(name)
        build.plugin?(name)
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

      def repository(id, url = nil, &block)
        repo = repositories.get(id, &block)
        repo.url = url if url
        repo
      end

      def plugin_repositories(&block)
        @plugin_repositories ||= ModelHash.new(PluginRepository)
        if block
          block.call(@plugin_repositories)
        end
        @plugin_repositories
      end

      def plugin_repository(id, url = nil, &block)
        repo = plugin_repositories.get(id, &block)
        repo.url = url if url
        repo
      end

      def dependency_management(&block)
        @dependency_management ||= DependencyManagement.new
        if block
          block.call(@dependency_management)
        end
        @dependency_management
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

      def profile(id, &block)
        profiles.get(id, &block)
      end

      def profiles(&block)
        @profiles ||= ModelHash.new(Profile)
        if block
          block.call(@profiles)
        end
        @profiles
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
          @property << Property.new(name, value)
        end
        self
      end

      def os(&block)
        @os ||= OS.new
        block.call(@os) if block
        @os
      end
      
      def by_default(value = true)
        @activeByDefault = value
        self
      end
    end

    class Property < Tag
      tags :name, :value

      def initialize(name, value)
        self.name name
        self.value value
      end
    end

    class Repository < Tag
      tags :id, :name, :url, :releases, :snapshots
      def initialize(id, &block)
        super({})
        self.id id
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
