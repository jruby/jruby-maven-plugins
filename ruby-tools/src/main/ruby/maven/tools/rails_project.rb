require File.join(File.dirname(__FILE__), 'gem_project.rb')
module Maven
  module Tools
    class RailsProject < GemProject
      tags :dummy

      def initialize(&block)
        super(dir_name, &block)
        self.group_id = "rails"
      end
      
      def add_defaults(args = {})
        self.name = "#{dir_name} - rails application" unless name
        self.packaging = "war" unless packaging

        super

        versions = { 
          :jetty_plugin => "7.2.2.v20101205", #7.1.0.RC1",
          :jruby_rack => "1.0.5",
          :war_plugin => "2.1.1",
        }.merge(args)
                
        jar("org.jruby.rack:jruby-rack", versions[:jruby_rack]) unless jar?("org.jruby.rack:jruby-rack")

        self.properties = {
          "jetty.version" => versions[:jetty_plugin],
          "rails.env" => "development",
        }.merge(self.properties)

        plugin(:rails3) do |rails|
          rails.version = "${jruby.plugins.version}" unless rails.version
          rails.extensions = true if rails.extensions.nil?
          rails.execution(:initialize).goals << "initialize"
        end

        plugin(:war, versions[:war_plugin]) unless plugin?(:war)
        plugin(:war).with({
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
          })

        profile(:development).activation.by_default
        profile(:test).activation.property("rails.env", "test")
        profile(:production) do |prod|   
          prod.activation.property("rails.env", "production")
          prod.properties = { 
            "gem.home" => "${project.build.directory}/rubygems-production", 
            "gem.path" => "${project.build.directory}/rubygems-production" 
          }.merge(prod.properties)
        end

        profile(:war).plugin("org.mortbay.jetty.jetty-maven-plugin",
                             "${jetty.version}")
         
        profile(:run) do |run|
            run.activation.by_default
            run.plugin("org.mortbay.jetty.jetty-maven-plugin", 
                       "${jetty.version}").with({
                :webAppConfig => {
                  :overrideDescriptor => '${project.build.directory}/jetty/override-${rails.env}-web.xml'
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
      end
    end
  end
end

if $0 == __FILE__
  proj = Maven::Tools::RailsProject.new
  proj.load(File.new(ARGV[0] || 'Gemfile'))
  proj.load(File.new(ARGV[1] || 'maven.rb'))
  proj.add_defaults
  puts proj.to_xml
end
