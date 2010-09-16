require File.join(File.dirname(__FILE__), 'model.rb')
require File.join(File.dirname(__FILE__), 'gemfile_reader.rb')

module Maven
  module Tools
    class RailsPom

      def initialize(args = {})
        @versions = { :jetty_plugin => "7.1.0.RC1",
                       :jruby_complete => "1.5.2",
                       :jruby_rack => "1.0.4.dev-SNAPSHOT",
                       :jruby_plugins => "0.22.0-SNAPSHOT",
                       :war_plugin => "2.1"
                     }.merge(args)
      end

      def check_rails(file)
        raise "it is not rails" unless File.exists?(File.join(File.dirname(file), "config", "application.rb"))
      end

      def create_pom(file)
        if file.is_a? File
          check_rails(file) 
          name = File.dirname(file).sub(/.*[\/\\]/, '')
        else
          name = File.basename(File.expand_path ".")
        end
        gemfile = GemfileReader.new(file)

        groups = gemfile.groups.dup
        groups[:production] ||= []
        groups[:test] ||= []
        groups[:development] ||= []
        defaultGroup = groups.delete(:default)
          
        GemProject.new(name) do |proj|
          proj.name = "#{name} - rails application"
          proj.packaging = "war"
          
          proj.repositories do |repos|
            repos.new("rubygems-releases") do |r|
              r.url = "http://gems.saumya.de/releases"
            end
            # repos.new("java-net") do |java|
            #   java.url='http://download.java.net/maven/2'
            #   java.releases(:enabled => true)
            # end
            if @versions[:jruby_rack] =~ /SNAPSHOT/
              repos.new("saumya") do |saumya|
                saumya.url = "http://mojo.saumya.de/"
                saumya.releases(:enabled => false)
                saumya.snapshots(:enabled => true, :updatePolicy => :never)
              end
            end
            if @versions[:jruby_plugins] =~ /SNAPSHOT/
              repos.new("sonatype-nexus-snapshots") do |nexus|
                nexus.url = "http://oss.sonatype.org/content/repositories/snapshots"
                nexus.releases(:enabled => false)
                nexus.snapshots(:enabled => true)
              end
            end
          end
          # proj.plugin_repositories do |repos|
          #   repos.new("java-net") do |java|
          #     java.url='http://download.java.net/maven/2'
          #   end
          # end

          proj.dependencies do |deps|
            defaultGroup.each do |dep|
              deps << dep
              # if dep.type == :gem
              #  else
              #  deps << Dependency.new( dep[0].sub(/\.[^.]+$/, ''), 
              #                          dep[0].sub(/.*\./, ''), 
              #                          dep[1])
              #end
            end
            deps << Dependency.new("org.jruby.rack", "jruby-rack", @versions[:jruby_rack])
            deps << Dependency.new("org.jruby", "jruby-complete", @versions[:jruby_complete])
          end
          
          proj.profiles.get(:development).activation.default
          proj.profiles.get(:production) do |prod|
            prod.properties = { 
              "gem.home" => "${project.build.directory}/rubygems-production", 
              "gem.path" => "${project.build.directory}/rubygems-production" 
            }
          end
             
          groups.each do |n, g|
            proj.profiles.get(n.to_s) do |profile|
              profile.activation(:name => "rails.env", :value => n.to_s)
              g.each do |gem|
                profile.dependencies << gem
              end
            end
          end
          
          proj.properties = {
            "project.build.sourceEncoding" => "UTF-8", 
            "gem.home" => "${project.build.directory}/rubygems", 
            "gem.path" => "${project.build.directory}/rubygems", 
            "jruby.plugins.version" => "#{@versions[:jruby_plugins]}", 
            "jetty.version" => "#{@versions[:jetty_plugin]}",
            "rails.env" => "development"
          }

          proj.build.plugins do |plugins|
           #  plugins.get_jruby(:gem) do |gem| 
#               gem.extensions = true
#               gem.executions.get("gemfile")do |gemfile|

#                 gemfile.configuration = {
#                   :script => 
# <<-EOF

# 		require 'fileutils'
# 		web_inf = File.join('${project.build.directory}', '${project.build.finalName}', 'WEB-INF')
# 		FileUtils.mkdir_p(web_inf)
# 		FileUtils.cp('Gemfile', File.join(web_inf, 'Gemfile'))
# EOF
#                 }
#               end
#               gemfile.phase = 'prepare-package'
#               gemfile.goals << 'exec'
#             end
            plugins.get_jruby(:rails3) do |rails|
              rails.extensions = true
              rails.executions.get(:initialize) do |e|
                e.goals = ["initialize"]
              end
            end
            plugins.get_maven("war", @versions[:war_plugin]) do |war|
              war.configuration = {
                :webResources => NamedArray.new(:resource) do |l|
                  l << { :directory => "public" }
                  l << { 
                    :directory => ".",
                    :targetPath => "WEB-INF",
                    :includes => ['app/**', 'config/**', 'lib/**', 'vendor/**', 'Gemfile']
                  }
                  l << {
                    :directory => '${gem.path}',
                    :targetPath => 'WEB-INF/gems'
                  }
                end
              }
            end
          end
          proj.profiles.new(:war).build.plugins.new("org.mortbay.jetty", 
                                                           "jetty-maven-plugin",                                                           "${jetty.version}")
          proj.profiles.new(:run) do |run|
            run.activation.default
            run.build.plugins.new("org.mortbay.jetty", 
                                  "jetty-maven-plugin", 
                                  "${jetty.version}") do |jetty|
              jetty.configuration = {
                :webAppConfig => {
                  :overrideDescriptor => 'src/main/jetty/override-${rails.env}-web.xml'
                },
                # :systemProperties => {
                #   :systemProperty => {
                #     :name => 'bundle.gemfile',
                #     :value => 'Gemfile.maven'
                #   }
                # }
              }
            end
          end
        end
      end
    end
  end
end

if $0 == __FILE__
  if ARGV[0].nil?
    Maven::Tools::RailsPom.new()
  else
    Maven::Tools::RailsPom.new(eval(ARGV[0]))
  end
end
