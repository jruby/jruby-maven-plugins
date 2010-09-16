require File.join(File.dirname(__FILE__), 'model.rb')

pom = Maven::Tools::GemProject.new("killer-app") do |proj|
  versions = { :jetty_plugin => "7.1.0.RC1",
    :jruby_complete => "1.5.2",
    :jruby_rack => "1.0.4.dev-SNAPSHOT",
    :jruby_plugins => "0.22.0-SNAPSHOT",
    :war_plugin => "2.1"
  }
  proj.name = "killer application"
  proj.packaging = "war"
  
  proj.repositories do |repos|
    repos.new("rubygems-releases") do |r|
      r.url = "http://gems.saumya.de/releases"
    end
    repos.new("saumya") do |saumya|
      saumya.url = "http://mojo.saumya.de/"
      saumya.releases(:enabled => false)
      saumya.snapshots(:enabled => true, :updatePolicy => :never)
    end
    repos.new("sonatype-nexus-snapshots") do |nexus|
      nexus.url = "http://oss.sonatype.org/content/repositories/snapshots"
      nexus.releases(:enabled => false)
      nexus.snapshots(:enabled => true)
    end
  end
  proj.plugin_repositories do |repos|
    repos.new("java-net") do |java|
      java.url='http://download.java.net/maven/2'
    end
  end
  
  proj.dependencies do |deps|
    deps << ["rails", "3.0.0"]
    deps << Maven::Tools::Dependency.new("org.jruby.rack", "jruby-rack", versions[:jruby_rack])
    deps << ["org.jruby.jruby-complete", versions[:jruby_complete]]
  end
  
  proj.profiles.get(:development).activation.default
  proj.profiles.get(:production) do |prod|
    prod.properties = { 
      "gem.home" => "${project.build.directory}/rubygems-production", 
      "gem.path" => "${project.build.directory}/rubygems-production" 
    }
  end
  
  proj.properties = {
    "project.build.sourceEncoding" => "UTF-8", 
    "gem.home" => "${project.build.directory}/rubygems", 
    "gem.path" => "${project.build.directory}/rubygems", 
    "jruby.plugins.version" => "#{versions[:jruby_plugins]}", 
    "jetty.version" => "#{versions[:jetty_plugin]}",
    "rails.env" => "development"
  }
  
  proj.build.plugins do |plugins|
    plugins.get_jruby(:gem) do |gem| 
      gem.extensions = true
      gem.executions.get("gemfile")do |gemfile|
        gemfile.configuration = {
          :script => 
          <<-EOF

		require 'fileutils'
		web_inf = File.join('${project.build.directory}', '${project.build.finalName}', 'WEB-INF')
		FileUtils.mkdir_p(web_inf)
		FileUtils.cp('Gemfile', File.join(web_inf, 'Gemfile'))
EOF
        }
        gemfile.phase = 'prepare-package'
        gemfile.goals << 'exec'
      end
    end
    plugins.get_jruby(:rails3) do |rails|
      rails.extensions = true
      rails.executions.get(:initialize) do |e|
        e.goals = ["initialize"]
      end
    end
    plugins.get_maven("war", versions[:war_plugin]) do |war|
      war.configuration = {
        :webResources => Maven::Tools::NamedArray.new(:resource) do |resource|
          resource << { :directory => "public" }
          resource << { 
            :directory => ".",
            :targetPath => "WEB-INF",
            :includes => ['app/**', 'config/**', 'lib/**', 'vendor/**', 'Gemfile']
          }
          resource << {
            :directory => '${gem.path}',
            :targetPath => 'WEB-INF/gems'
          }
        end
      }
    end
  end
  proj.profiles.new(:war).build.plugins.new("org.mortbay.jetty", 
                                            "jetty-maven-plugin",                                                               "${jetty.version}")
  proj.profiles.new(:run) do |run|
    run.activation.default
    run.build.plugins.new("org.mortbay.jetty", 
                          "jetty-maven-plugin", 
                          "${jetty.version}") do |jetty|
      jetty.configuration = {
        :webAppConfig => {
          :overrideDescriptor => 'src/main/jetty/override-${rails.env}-web.xml'
        },
        :systemProperties => {
          :systemProperty => {
            :name => 'bundle.gemfile',
            :value => 'Gemfile.maven'
          }
        }
      }
    end
  end
end
puts pom.to_xml
