require File.join(File.dirname(__FILE__), 'gem_project.rb')
module Maven
  module Tools
    class RailsProject < GemProject
      tags :dummy

      def initialize(name = dir_name, &block)
        super(name, &block)
        group_id "rails"
        packaging "war"
      end
      
      def add_defaults(args = {})
        self.name = "#{dir_name} - rails application" unless name
        
        # setup bundler plugin
        plugin(:bundler)

        s_args = args.dup
        s_args.delete(:jruby_plugins)
        super(s_args)

        versions = VERSIONS.merge(args)
        
        rails_gem = dependencies.detect { |d| d.type.to_sym == :gem && d.artifact_id.to_s =~ /^rail.*s$/ } # allow rails or railties

        if rails_gem && rails_gem.version =~ /^3.1./
          versions[:jruby_rack] = '1.1.0.dev'
        end

        if !jar?("org.jruby:jruby-complete") && !jar?("org.jruby:jruby-core") && versions[:jruby_version]
          minor = versions[:jruby_version].sub(/[0-9]*\./, '').sub(/\..*/, '')

          #TODO once jruby-core pom is working !!!
          if minor.to_i > 55 #TODO fix minor minimum version
            jar("org.jruby:jruby-core", versions[:jruby_version])
            jar("org.jruby:jruby-stdlib", versions[:jruby_version])
            # override deps which works
            jar("jline:jline", '0.9.94') if versions[:jruby_version] =~ /1.6.[1-2]/
            jar("org.jruby.extras:jffi", '1.0.8', 'native') if versions[:jruby_version] =~ /1.6.[0-2]/
            jar("org.jruby.extras:jaffl", '0.5.10') if versions[:jruby_version] =~ /1.6.[0-2]/
          else
            jar("org.jruby:jruby-complete", versions[:jruby_version]) 
          end
        end

        jar("org.jruby.rack:jruby-rack", versions[:jruby_rack]) unless jar?("org.jruby.rack:jruby-rack")

        self.properties = {
          "jetty.version" => versions[:jetty_plugin],
          "rails.env" => "development",
          "gem.includeRubygemsInTestResources" => false
        }.merge(self.properties)

        plugin(:rails3) do |rails|
          rails.version = "${jruby.plugins.version}" unless rails.version
          rails.in_phase(:validate).execute_goal(:initialize)#.goals << "initialize"
        end

        plugin(:war, versions[:war_plugin]) unless plugin?(:war)
        plugin(:war).with({
            :webResources => Maven::Model::NamedArray.new(:resource) do |l|
              l << { :directory => "public" }
              l << { 
                :directory => ".",
                :targetPath => "WEB-INF",
                :includes => ['app/**', 'config/**', 'lib/**', 'vendor/**', 'Gemfile']
              }
              l << {
                :directory => '${gem.path}',
                :targetPath => 'WEB-INF/gems',
                :includes => ['gems/**', 'specifications/**']
              }
              l << {
                :directory => '${gem.path}-bundler-maven-plugin',
                :targetPath => 'WEB-INF/gems',
                :includes => ['specifications/**']
              }
            end
          })

        profile(:assets).activation.by_default if profiles.key?(:assets)
        profile(:development).activation.by_default
        profile(:test).activation.property("rails.env", "test")
        profile(:production) do |prod|   
          prod.activation.property("rails.env", "production")
          prod.properties = { 
            "gem.home" => "${project.build.directory}/rubygems-production", 
            "gem.path" => "${project.build.directory}/rubygems-production" 
          }.merge(prod.properties)
        end

        profile(:war).plugin("org.mortbay.jetty:jetty-maven-plugin",
                             "${jetty.version}")
         
        profile(:run) do |run|
          overrideDescriptor = '${project.build.directory}/jetty/override-${rails.env}-web.xml'
          run.activation.by_default
          run.plugin("org.mortbay.jetty:jetty-maven-plugin",
                       "${jetty.version}").with({
                :webAppConfig => {
                  :overrideDescriptor => overrideDescriptor           
                },
                :connectors => <<-XML

                <connector implementation="org.eclipse.jetty.server.nio.SelectChannelConnector">
                  <port>8080</port>
                </connector>
                <connector implementation="org.eclipse.jetty.server.ssl.SslSelectChannelConnector">
                  <port>8443</port>
                  <keystore>${project.basedir}/src/test/resources/server.keystore</keystore>
                  <keyPassword>123456</keyPassword>
                  <password>123456</password>
                </connector>
XML
              })
        end
        profile(:executable) do |exec|
          exec.plugin_repository('kos').url = 'http://opensource.kantega.no/nexus/content/groups/public/'
          exec.plugin('org.simplericity.jettyconsole:jetty-console-maven-plugin', '1.42').execution do |jetty|
            jetty.execute_goal(:createconsole)
            jetty.configuration.comment <<-TEXT
                  see http://simplericity.com/2009/11/10/1257880778509.html for more info
                -->
                <!--
		  <backgroundImage>${basedir}/src/main/jettyconsole/puffin.jpg</backgroundImage>
		  <additionalDependencies>
		    <additionalDependency>
		      <artifactId>jetty-console-winsrv-plugin</artifactId>
		    </additionalDependency>
		    <additionalDependency>
		      <artifactId>jetty-console-requestlog-plugin</artifactId>
		    </additionalDependency>
		    <additionalDependency>
		      <artifactId>jetty-console-log4j-plugin</artifactId>
		    </additionalDependency>
		    <additionalDependency>
		      <artifactId>jetty-console-jettyxml-plugin</artifactId>
		    </additionalDependency>
		    <additionalDependency>
		      <artifactId>jetty-console-ajp-plugin</artifactId>
		    </additionalDependency>
		    <additionalDependency>
		      <artifactId>jetty-console-gzip-plugin</artifactId>
		    </additionalDependency>
		    <additionalDependency>
		      <artifactId>jetty-console-startstop-plugin</artifactId>
		    </additionalDependency>
		  </additionalDependencies>
TEXT
          end
        end
      end
    end
  end
end

if $0 == __FILE__
  proj = Maven::Tools::RailsProject.new
  proj.load(ARGV[0] || 'Gemfile')
  proj.load(ARGV[1] || 'Mavenfile')
  proj.add_defaults
  puts proj.to_xml
end
