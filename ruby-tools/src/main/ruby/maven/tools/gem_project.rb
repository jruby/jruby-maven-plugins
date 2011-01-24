require File.join(File.dirname(__FILE__), 'model.rb')
require File.join(File.dirname(__FILE__), 'gemfile_lock.rb')
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

      def dump_loaded_file_list
        if loaded_files.size > 0
          File.open(loaded_files[0] + ".files", 'w') do |f|
            loaded_files.each { |i| f.puts i }
          end
        end
      end

      def load(file)
        file = file.path if file.is_a?(File)
        if File.exists? file
          content = File.read(file)
          loaded_files << file
          if @lock.nil?
            @lock = GemfileLock.new(file + ".lock")
            loaded_files << file + ".lock"
            @lock.hull.each do |dep|
              dependency_management << dep
            end
          end
          eval "merge do\n#{content}\nend"
        else
          self
        end
      end

      def dir_name
        File.basename(File.expand_path("."))
      end

      def add_defaults(args = {})
        versions = { 
          :jruby_complete => "1.5.6",
          :jruby_plugins => "0.24.0",
        }.merge(args)

        self.name = "#{dir_name} - gem" unless name
        
        self.packaging = "gem" unless packaging

        repository("rubygems-releases").url = "http://gems.saumya.de/releases" unless repository("rubygems-releases").url
        
        jar("org.jruby:jruby-complete", versions[:jruby_complete]) unless jar?("org.jruby:jruby-complete")

        # TODO go through all plugins to find outany SNAPSHOT version !!
        if versions[:jruby_plugins] =~ /SNAPSHOT/
          repository("sonatype-nexus-snapshots") do |nexus|
            nexus.url = "http://oss.sonatype.org/content/repositories/snapshots"
            nexus.releases(:enabled => false)
            nexus.snapshots(:enabled => true)
          end
        end

        if plugin?("gem")
          plugin("gem").extensions = true
        elsif versions[:jruby_plugins] 
          plugin("gem", "${jruby.plugins.version}").extensions = true
        end

        if versions[:jruby_plugins]
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
          has_gem = gem?(name)
          plugin(name, "${jruby.plugins.version}").execution.goals = "test" if has_gem || File.exists?(test_dir)
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
        # take last from stack
        stack.last.each do |c|
          ref = 
            case c
            when :default
              self
            else
              profile(c)
            end
          ref.dependencies << args
        end
      end

      def gem(*args)
        if(args[0] =~ /:/)
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
            dep = ref.dependencies << args[0]
            if @lock.empty?
              dependency_management << args
            else
              @lock.dependency_hull(args[0]).map.each do |d|
                ref.dependencies << d
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
  proj.load(File.new(ARGV[0] || 'Gemfile'))
  proj.load(File.new(ARGV[1] || 'maven.rb'))
  proj.add_defaults
  puts proj.to_xml
end
