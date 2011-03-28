require File.join(File.dirname(__FILE__), '..', 'model', 'model.rb')
require File.join(File.dirname(__FILE__), 'gemfile_lock.rb')
require File.join(File.dirname(__FILE__), 'versions.rb')

module Maven
  module Tools
    class GemProject < Maven::Model::Project
      tags :dummy

      def initialize(artifact_id = dir_name, version = "0.0.0", &block)
        super("rubygems", artifact_id, version, &block)
        packaging "gem"
      end

      def loaded_files
        @files ||= []
      end

      def current_file
        loaded_files.last
      end

      def dump_loaded_file_list
        if loaded_files.size > 0
          basedir = File.dirname(loaded_files[0])
          File.open(loaded_files[0] + ".files", 'w') do |f|
            loaded_files.each { |i| f.puts i.sub(/^#{basedir}./, '') }
          end
        end
      end

      def add_param(config, name, list, default = [])
        if list.is_a? Array
          config[name] = list.join(",").to_s unless (list || []) == default
        else
          # list == nil => (list || []) == default is true
          config[name] = list.to_s unless (list || []) == default
        end
      end
      private :add_param

      def load_gemspec(specfile)
        require 'rubygems'
        spec = ::Gem::Specification.load(specfile)
        raise "file not found '#{specfile}'" unless spec
        loaded_files << File.expand_path(specfile)
        artifact_id spec.name
        version spec.version
        name spec.summary || "#{self.artifact_id} - gem"
        description spec.description if spec.description
        url spec.homepage if spec.homepage
        (spec.email || []).zip(spec.authors || []).map do |email, author|
          self.developers.new(author, email)
        end

        #  TODO work with collection of licenses - there can be more than one !!!
        (spec.licenses + spec.files.select {|file| file.to_s =~ /license|gpl/i }).each do |license|
          # TODO make this better, i.e. detect the right license name from the file itself
          self.licenses.new(license)
        end

        
        config = {}
        add_param(config, "autorequire", spec.autorequire)
        add_param(config, "defaultExecutable", spec.default_executable)
        add_param(config, "requirements", spec.requirements)
        add_param(config, "testFiles", spec.test_files)
        #has_rdoc always gives true => makes not sense to keep it then
        #add_param(config, "hasRdoc", spec.has_rdoc)
        add_param(config, "extraRdocFiles", spec.extra_rdoc_files)
        add_param(config, "rdocOptions", spec.rdoc_options)
        add_param(config, "requirePaths", spec.require_paths, ["lib"])
        add_param(config, "rubyforgeProject", spec.rubyforge_project)
        add_param(config, "requiredRubygemsVersion", spec.required_rubygems_version, ">= 0")
        add_param(config, "bindir", spec.bindir, "bin")
        add_param(config, "requiredRubyVersion", spec.required_ruby_version, ">= 0")
        add_param(config, "postInstallMessage", spec.post_install_message ? "<![CDATA[#{spec.post_install_message}]]>" : nil)
        add_param(config, "executables", spec.executables)
        add_param(config, "extensions", spec.extensions)
        add_param(config, "platform", spec.platform, 'ruby')

        # # TODO maybe calculate extra files
        # files = spec.files.dup
        # (Dir['lib/**/*'] + Dir['spec/**/*'] + Dir['features/**/*'] + Dir['test/**/*'] + spec.licenses + spec.extra_rdoc_files).each do |f|
        #   files.delete(f)
        #   if f =~ /^.\//
        #     files.delete(f.sub(/^.\//, ''))
        #   else
        #     files.delete("./#{f}")
        #   end
        # end
        #add_param(config, "extraFiles", files)
        add_param(config, "files", spec.files)
        
        plugin('gem').with(config) if config.size > 0

        spec.dependencies.each do |dep|
          scope = 
            case dep.type
            when :runtime
              "compile"
            when :development
              "test"
            else
              warn "unknown scope: #{dep.type}"
              "compile"
            end

          versions = dep.requirement.requirements.collect do |req|
            req.to_s
          end
          gem(dep.name, versions).scope = scope
        end
      end

      def load(file)
        file = file.path if file.is_a?(File)
        if File.exists? file
          content = File.read(file)
          loaded_files << file
          if @lock.nil?
            @lock = GemfileLock.new(file + ".lock")
            if @lock.size == 0
              @lock = nil
            else
              loaded_files << file + ".lock"
              @lock.hull.each do |dep|
                dependency_management.gem dep
              end
            end
          end
          eval content
        else
          self
        end
      end

      def dir_name
        File.basename(File.expand_path("."))
      end
      private :dir_name

      def add_defaults(args = {})
        versions = VERSIONS
        versions = versions.merge(args) if args

        name "#{dir_name} - gem" unless name
        
        packaging "gem" unless packaging

        repository("rubygems-releases").url = "http://gems.saumya.de/releases" unless repository("rubygems-releases").url
        
        jar("org.jruby:jruby-complete", versions[:jruby_version]) unless jar?("org.jruby:jruby-complete")

        # TODO go through all plugins to find out any SNAPSHOT version !!
        if versions[:jruby_plugins] =~ /-SNAPSHOT$/ || properties['jruby.plugins.version'] =~ /-SNAPSHOT$/
          plugin_repository("sonatype-snapshots") do |nexus|
            nexus.url = "http://oss.sonatype.org/content/repositories/snapshots"
            nexus.releases(:enabled => false)
            nexus.snapshots(:enabled => true)
          end
        end

        if packaging =~ /gem/ || plugin?(:gem)
          gem = plugin(:gem)
          gem.version = "${jruby.plugins.version}" unless gem.version
          gem.extensions = true if packaging =~ /gem/
        end

        if plugin?(:bundler)
          bundler = plugin(:bundler)
          bundler.version = "${jruby.plugins.version}" unless bundler.version
          plugin(:bundler, "${jruby.plugins.version}").executions.goals << "install"
          unless gem?(:bundler)
            gem("bundler")
          end
        end

        if versions[:jruby_plugins]
          #add_test_plugin(nil, "test")
          add_test_plugin("rspec", "spec")
          add_test_plugin("cucumber", "features")
        end

        self.properties = {
          "project.build.sourceEncoding" => "UTF-8", 
          "gem.home" => "${project.build.directory}/rubygems", 
          "gem.path" => "${project.build.directory}/rubygems",
          "jruby.plugins.version" => versions[:jruby_plugins]
        }.merge(self.properties)
      end

      def add_test_plugin(name, test_dir)
        unless plugin?(name)
          has_gem = name.nil? ? true : gem?(name)
          if has_gem && File.exists?(test_dir)
            plugin(name || 'runit', "${jruby.plugins.version}").execution.goals << "test" 
          end
        else
          pl = plugin(name || 'runit')
          pl.version = "${jruby.plugins.version}" unless pl.version
        end
      end
      private :add_test_plugin

      def stack
        @stack ||= [[:default]]
      end
      private :stack
        
      def group(*args, &block)
        stack << args
        block.call if block
        stack.pop
      end

      def gemspec(name = nil)
        if name
          load_gemspec(File.join(File.dirname(current_file), name))
        else
          Dir[File.join(File.dirname(current_file), "*.gemspec")].each do |file|
            load_gemspec(file)
          end
        end
      end

      def source(*args)
        warn "ignore source #{args}" if !(args[0].to_s =~ /^https?:\/\/rubygems.org/) && args[0] != :rubygems
      end

      def path(*args)
      end

      def git(*args)
      end

      def is_jruby_platform(*args)
        args.detect { |a| :jruby == a.to_sym }
      end
      private :is_jruby_platform

      def platforms(*args, &block)
        if is_jruby_platform(*args)
          block.call
        end
      end

      def gem(*args, &block)
        dep = nil
        if args.last.is_a?(Hash)
          options = args.delete(args.last)
          unless options.key?(:git) || options.key?(:path)
            if options[:platforms].nil? || is_jruby_platform(*(options[:platforms] || []))
              group = options[:group] || options[:groups]
              if group
                [group].flatten.each do |g|
                  if dep
                    profile(g).dependencies << dep
                  else
                    dep = profile(g).gem(args, &block)
                  end
                end
              else
                self.gem(args, &block)
              end
            end
          end
        else
          stack.last.each do |c|
            if c == :default
              if @lock.nil?
                dep = add_gem(args, &block)
              else
                dep = add_gem(args[0], &block)

                # add its dependencies as well to have the version
                # determine by the dependencyManagement
                @lock.dependency_hull(args[0]).map.each do |d|
                  add_gem d[0], nil
                end
              end
            else
              if @lock.nil?
                if dep
                  profile(c).dependencies << dep
                else
                  dep = profile(c).gem(args, &block)
                end
              else
                if dep
                  profile(c).dependencies << dep
                else
                  dep = profile(c).gem(args[0], nil, &block)
                end
                # add its dependencies as well to have the version
                # determine by the dependencyManagement
                @lock.dependency_hull(args[0]).map.each do |d|
                  profile(c).gem d[0], nil unless gem? d[0]
                end
              end
            end
          end
        end
        if (profiles.size > 0) && dep
          project = self
          
          plugin(:bundler) do |bundler|
            bundler.dependencies << dep unless project.dependencies.member? dep
            
            if @lock
              # add its dependencies as well to have the version
              # determine by the dependencyManagement
              @lock.dependency_hull(dep.artifact_id).map.each do |d|
                bundler.gem d[0], nil unless project.gem? d[0]
              end
            end
          end
        end
        dep
      end
    end
  end
end

if $0 == __FILE__
  proj = Maven::Tools::GemProject.new("test_gem")
  if ARGV[0] =~ /\.gemspec$/
    proj.load_gemspec(ARGV[0])
  else
    proj.load(ARGV[0] || 'Gemfile')
  end
  proj.load(ARGV[1] || 'Mavenfile')
  proj.add_defaults
  puts proj.to_xml
end
