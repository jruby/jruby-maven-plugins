require File.join(File.dirname(__FILE__), 'model.rb')
require File.join(File.dirname(__FILE__), 'gemfile_lock.rb')
require File.join(File.dirname(__FILE__), 'versions.rb')
module Maven
  module Tools
    class GemProject < Project
      tags :dummy

      def initialize(artifact_id, version = "0.0.0", &block)
        super("rubygems", artifact_id, version, &block)
      end

      def loaded_files
        @files ||= []
      end

      def current_file
        loaded_files.last
      end

      def dump_loaded_file_list
        if loaded_files.size > 0
          File.open(loaded_files[0] + ".files", 'w') do |f|
            loaded_files.each { |i| f.puts i }
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

      def load_gemspec(specfile)
        require 'rubygems'
        spec = ::Gem::Specification.load(specfile)
        self.artifact_id = spec.name
        self.version = spec.version
        self.packaging = spec.platform.to_s == 'java'? "java-gem" : "gem"
        self.name = spec.summary || "#{self.artifact_id} - gem"
        self.description = spec.description if spec.description
        self.url = spec.homepage if spec.homepage
        (spec.email || []).zip(spec.authors || []).map do |email, author|
          self.developers.new(author, email)
        end

        #  TODO work with collection of licenses - there can be more than one !!!
        (spec.licenses + spec.files.select {|file| file.to_s =~ /license|gpl/i }).each do |license|
          # TODO make this better, i.e. detect the right license name from the file itself
          self.licenses.new(license)
        end

        plugin?('gem') ? self.plugin('gem') : self.plugin('gem', "${jruby.plugins.version}") do |gem_plugin|
          gem_plugin.extensions = true
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
          add_param(config, "postInstallMessage", spec.post_install_message)
          add_param(config, "executables", spec.executables)
          add_param(config, "extensions", spec.extensions)
          add_param(config, "platform", spec.platform, 'ruby')

          # # calculate extra files
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

          gem_plugin.with(config) if config.size > 0
        end

        spec.dependencies.each do |dep|
          scope = case dep.type
                  when :runtime
                      "compile"
                  when :development
                    "test"
                  else
                    warn "unknown scope: #{dep.type}"
                    "compile"
                  end
          left_version = nil
          right_version = nil
          version = nil
          (0..(dep.requirement.requirements.size - 1)).each do |index|
            req = dep.requirement.requirements[index]
            gem_version = req[1].to_s
            gem_final_version = gem_version
            gem_final_version = gem_final_version + ".0" if gem_final_version =~ /^[0-9]+\.[0-9]+$/
            gem_final_version = gem_final_version + ".0.0" if gem_final_version =~ /^[0-9]+$/
            case req[0]
            when "="
              version = gem_final_version
            when ">="
              left_version = "[#{gem_final_version}"
            when ">"
              left_version = "(#{gem_final_version}"
            when "<="
              right_version = "#{gem_final_version}]"
            when "<"
              right_version = "#{gem_final_version})"
            when "~>"
              # TODO not sure if this makes sense for prereleases
              pre_version = gem_version.sub(/[.][a-zA-Z0-9]+$/, '')
              # hope the upper bound is "big" enough but needed, i.e.
              # version 4.0.0 is bigger than 4.0.0.pre and [3.0.0, 4.0.0) will allow
              # 4.0.0.pre which is NOT intended
              version = "[#{gem_version},#{pre_version.sub(/[0-9]+$/, '')}#{pre_version.sub(/.*[.]/, '').to_i}.99999)"
            else
              warn "not implemented comparator: #{req.inspect}"
            end
          end
          warn "having left_version or right_version and version which does not makes sense" if (right_version || left_version) && version
          version = (left_version || "[") + "," + (right_version || ")") if right_version || left_version
          version = "[0.0.0,)" if version.nil?

          #spec_tuples = @fetcher.find_matching dep, true, false, nil
          #is_java = spec_tuples.detect { |s| s[0][2] == 'java' }
          gem(dep.name, version).scope = scope
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
                dependency_management << dep
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

      def add_defaults(args = {})
        versions = VERSIONS
        versions.merge(args) if args

        self.name = "#{dir_name} - gem" unless name
        
        self.packaging = "gem" unless packaging

        repository("rubygems-releases").url = "http://gems.saumya.de/releases" unless repository("rubygems-releases").url
        
        jar("org.jruby:jruby-complete", versions[:jruby_complete]) unless jar?("org.jruby:jruby-complete")

        # TODO go through all plugins to find outany SNAPSHOT version !!
        if versions[:jruby_plugins] =~ /SNAPSHOT/
          plugin_repository("sonatype-nexus-snapshots") do |nexus|
            nexus.url = "http://oss.sonatype.org/content/repositories/snapshots"
            nexus.releases(:enabled => false)
            nexus.snapshots(:enabled => true)
          end
        end

        unless plugin?("gem")
          if versions[:jruby_plugins] 
            plugin("gem", "${jruby.plugins.version}").extensions = true
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
          plugin(name || 'runit', "${jruby.plugins.version}").execution.goals << "test" if has_gem || File.exists?(test_dir)
          if !has_gem && File.exists?(test_dir)
            gem(name, "[0.0.0,)").scope = :test
          end
        end
      end

      def plugin?(name)
        build.plugins.keys.member?(name) || profiles.detect do |id, profile|
          profile.build.plugins.keys.member?(name)
        end
      end
      protected :plugin?

      def jar?(name)
        artifact_id, group_id = name.sub(/:[^:]+$/, ''), name.sub(/.*:/, '')
        dependencies.member?(Dependency.new(artifact_id, group_id)) || profiles.detect do |id, profile|
          profile.dependencies.member?(Dependency.new(artifact_id, group_id))
        end
      end
      protected :jar?

      def gem?(gemname)
        dependencies.member?(Gem.new(gemname)) || profiles.detect do |id, profile|
          profile.dependencies.member?(Gem.new(gemname))
        end
      end
      protected :gem?

      def stack
        @stack ||= [[:default]]
      end
      private :stack
        
      def group(*args, &block)
        stack << args
        block.call if block
        stack.pop
      end

      def source(*args)
        warn "ignore source #{args}" if args[0] != 'http://rubygems.org'
      end

      def jar(*args)
        result = nil
        # take last from stack
        stack.last.each do |c|
          ref = 
            case c
            when :default
              self
            else
              profile(c)
            end
          if result
            ref.dependencies << result
          else
            result = ref.dependencies << args
          end
        end
        result
      end

      def gem(*args)
        if(args[0] =~ /^mvn:/)
          args[0].sub!(/^mvn:/, '')
          jar(*args)
        else
          dep = nil
          # take last from stack
          stack.last.each do |c|
            if args.last.is_a?(Hash) 
              if args.last[:git]
                # ignore git gem
                break
              else
                # omit options
                args.delete(args.last)
              end
            end
            ref = 
              case c
              when :default
                self
              else
                profile(c)
              end
            if @lock.nil?
              if dep
                ref.dependencies << dep
              else
                dep = ref.dependencies << args
              end
            else
              if dep
                ref.dependencies << dep
              else
                dep = ref.dependencies << args[0]
              end
              # add its dependencies as well to have the version
              # determine by the dependencyManagement
              @lock.dependency_hull(args[0]).map.each do |d|
                ref.dependencies << d[0]
              end
            end
          end
          dep
        end
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
