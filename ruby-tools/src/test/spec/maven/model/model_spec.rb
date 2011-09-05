require File.join(File.dirname(__FILE__), '..', '..', '..', '..', 'main', 'ruby', 'maven', 'model', 'model.rb')

describe Maven::Model do

  describe Maven::Model::Project do

    before :each do
      @project = Maven::Model::Project.new("test:project", '0.0.0')
    end

    it 'should setup a project with split args' do
      Maven::Model::Project.new("test", "project", "1.0.0").to_xml.should == <<-XML
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>test</groupId>
  <artifactId>project</artifactId>
  <version>1.0.0</version>
</project>
XML
    end
    
    it 'should setup a minimal project' do
      project = Maven::Model::Project.new
      project.artifact_id = 'mini'
      project.parent('test:parent', '1.2.3')
      project.to_xml.should == <<-XML
<project>
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>test</groupId>
    <artifactId>parent</artifactId>
    <version>1.2.3</version>
  </parent>
  <artifactId>mini</artifactId>
</project>
XML
    end
    
    it 'should setup a project with parent' do
      @project.parent("test:parent", '0.1.2').relative_path='../pom.rb'
      @project.to_xml.should == <<-XML
<project>
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>test</groupId>
    <artifactId>parent</artifactId>
    <version>0.1.2</version>
    <relativePath>../pom.rb</relativePath>
  </parent>
  <groupId>test</groupId>
  <artifactId>project</artifactId>
  <version>0.0.0</version>
</project>
XML
    end

    it 'should setup a project with metadata' do
      @project.name "name"
      @project.packaging = :jar
      @project.description <<-TXT
some text
more
TXT
      @project.url "http://example.com"
      @project.to_xml.should == <<-XML
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>test</groupId>
  <artifactId>project</artifactId>
  <version>0.0.0</version>
  <name><![CDATA[name]]></name>
  <packaging>jar</packaging>
  <description><![CDATA[some text
more
]]></description>
  <url>http://example.com</url>
</project>
XML
    end

    it 'should setup a project with developers' do
      @project.developers.new("my name1", "my_email1@example.com")
      @project.developers.new("my name2 <my_email2@example.com>")
      @project.to_xml.should == <<-XML
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>test</groupId>
  <artifactId>project</artifactId>
  <version>0.0.0</version>
  <developers>
    <developer>
      <id>my_email1_at_example_dot_com</id>
      <name>my name1</name>
      <email>my_email1@example.com</email>
    </developer>
    <developer>
      <id>my_email2_at_example_dot_com</id>
      <name>my name2</name>
      <email>my_email2@example.com</email>
    </developer>
  </developers>
</project>
XML
    end

    it 'should setup a project with licenses' do
      @project.licenses.new("MIT-LICENSE.txt")
      @project.licenses.new("http://www.gnu.org/licenses/gpl.html")
      @project.to_xml.should == <<-XML
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>test</groupId>
  <artifactId>project</artifactId>
  <version>0.0.0</version>
  <licenses>
    <license>
      <name>MIT-LICENSE</name>
      <url>./MIT-LICENSE.txt</url>
      <distribution>repo</distribution>
    </license>
    <license>
      <name>gpl</name>
      <url>http://www.gnu.org/licenses/gpl.html</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
</project>
XML
    end

    it 'should setup a project with repositories' do
      @project.repository("rubygems-releases", "http://rubygems-proxy.torquebox.org/releases")
      @project.plugin_repository("sonatype-snapshots") do |sonatype|
        sonatype.url "http://oss.sonatype.org/content/repositories/snapshots"
        sonatype.releases(:enabled => false)
        sonatype.snapshots(:enabled => true)
      end
      @project.repository("jboss-public-repository-group") do |jboss|
        jboss.name "JBoss Public Maven Repository Group"
        jboss.url "https://repository.jboss.org/nexus/content/groups/public-jboss/"
        jboss.releases(:enabled => false, :updatePolicy => :never, :checksumPolicy => :strict)
        jboss.snapshots(:enabled => true, :updatePolicy => :daily, :checksumPolicy => :ignore)
      end
      @project.to_xml.should == <<-XML
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>test</groupId>
  <artifactId>project</artifactId>
  <version>0.0.0</version>
  <repositories>
    <repository>
      <id>rubygems-releases</id>
      <url>http://rubygems-proxy.torquebox.org/releases</url>
    </repository>
    <repository>
      <id>jboss-public-repository-group</id>
      <name>JBoss Public Maven Repository Group</name>
      <url>https://repository.jboss.org/nexus/content/groups/public-jboss/</url>
      <releases>
        <enabled>false</enabled>
        <updatePolicy>never</updatePolicy>
        <checksumPolicy>strict</checksumPolicy>
      </releases>
      <snapshots>
        <enabled>true</enabled>
        <updatePolicy>daily</updatePolicy>
        <checksumPolicy>ignore</checksumPolicy>
      </snapshots>
    </repository>
  </repositories>
  <pluginRepositories>
    <pluginRepository>
      <id>sonatype-snapshots</id>
      <url>http://oss.sonatype.org/content/repositories/snapshots</url>
      <releases>
        <enabled>false</enabled>
      </releases>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
    </pluginRepository>
  </pluginRepositories>
</project>
XML
    end

    it 'should setup a project with properties' do
      @project.properties["gem.home"] = "${project.build.directory}/rubygems"
      @project.properties = { "gem.home" => "${user.home}/.gem/jruby/1.8" }
      @project.properties["gem.path"] = "${project.build.directory}/rubygems"
      @project.properties["jruby.plugins.version"] = "0.26.0-SNAPSHOT"
      @project.properties["project.build.sourceEncoding"] = "UTF-8"
      @project.properties.merge!({
                                   "gem.path" => "${user.home}/.gem/jruby/1.8"
                                 })
      @project.to_xml.should == <<-XML
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>test</groupId>
  <artifactId>project</artifactId>
  <version>0.0.0</version>
  <properties>
    <gem.home>${user.home}/.gem/jruby/1.8</gem.home>
    <gem.path>${user.home}/.gem/jruby/1.8</gem.path>
    <jruby.plugins.version>0.26.0-SNAPSHOT</jruby.plugins.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
</project>
XML
    end

    it 'should setup a project with dependencies and dependency_management' do
      @project.dependency_management do |deps|
        deps.gem "slf4r", "0.4.2"
        deps.jar "org.slf4j:slf4j-simple", "1.6.2"
      end
      @project.dependency_management.gem "rspec", "2.4.1"
      @project.dependencies do |d| 
       d.gem "rspec"
       d.test_jar "org.slf4j:slf4j-noop", "1.6.2"
     end
      @project.gem "rspec"
      @project.test_jar "org.slf4j:slf4j-noop", "1.6.2"
      @project.to_xml.should == <<-XML
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>test</groupId>
  <artifactId>project</artifactId>
  <version>0.0.0</version>
  <dependencies>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>rspec</artifactId>
      <version>[0.0.0,)</version>
      <type>gem</type>
    </dependency>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-noop</artifactId>
      <version>1.6.2</version>
      <type>jar</type>
      <scope>test</scope>
    </dependency>
  </dependencies>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>slf4r</artifactId>
        <version>0.4.2</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>1.6.2</version>
        <type>jar</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>rspec</artifactId>
        <version>2.4.1</version>
        <type>gem</type>
      </dependency>
    </dependencies>
  </dependencyManagement>
</project>
XML
    end

    it 'should setup a project with dependency with exclusions' do
      @project.jar 'org.xerial:sqlite-jdbc', '3.6.10' do |j|
        j.exclude 'org.xerial.thirdparty:nestedvm'
      end
      @project.gem 'sqlite-jdbc', '3.6.10'
      @project.gem('sqlite-jdbc').exclude 'nestedvm'
      @project.to_xml.should == <<-XML
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>test</groupId>
  <artifactId>project</artifactId>
  <version>0.0.0</version>
  <dependencies>
    <dependency>
      <groupId>org.xerial</groupId>
      <artifactId>sqlite-jdbc</artifactId>
      <version>3.6.10</version>
      <type>jar</type>
      <exclusions>
        <exclusion>
          <groupId>org.xerial.thirdparty</groupId>
          <artifactId>nestedvm</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>sqlite-jdbc</artifactId>
      <version>3.6.10</version>
      <type>gem</type>
      <exclusions>
        <exclusion>
          <groupId>rubygems</groupId>
          <artifactId>nestedvm</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
  </dependencies>
</project>
XML
    end

    it 'should allow to set maven core plugins directly in the project' do
      @project.plugin 'release'
      @project.plugin("clean", "2.4.1")
      @project.plugin(:compile, "2.3.2").with :source => "1.5", :target => "1.5"
      @project.to_xml.should == <<-XML
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>test</groupId>
  <artifactId>project</artifactId>
  <version>0.0.0</version>
  <build>
    <plugins>
      <plugin>
        <artifactId>maven-release-plugin</artifactId>
      </plugin>
      <plugin>
        <artifactId>maven-clean-plugin</artifactId>
        <version>2.4.1</version>
      </plugin>
      <plugin>
        <artifactId>maven-compile-plugin</artifactId>
        <version>2.3.2</version>
        <configuration>
          <source>1.5</source>
          <target>1.5</target>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
XML
    end

    it 'should allow to set jruby plugins directly in the project' do
      @project.plugin(:gem).extensions true
      @project.plugin(:gem).in_phase("pre-integration-test").execute_goal(:install)
      @project.plugin(:cucumber).in_phase("integration-test").execute_goal(:test).with(:cucumberArgs => "--no-colors")
      @project.to_xml.should == <<-XML
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>test</groupId>
  <artifactId>project</artifactId>
  <version>0.0.0</version>
  <build>
    <plugins>
      <plugin>
        <groupId>de.saumya.mojo</groupId>
        <artifactId>gem-maven-plugin</artifactId>
        <extensions>true</extensions>
        <executions>
          <execution>
            <id>in_phase_pre_integration_test</id>
            <phase>pre-integration-test</phase>
            <goals>
              <goal>install</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>de.saumya.mojo</groupId>
        <artifactId>cucumber-maven-plugin</artifactId>
        <executions>
          <execution>
            <id>in_phase_integration_test</id>
            <phase>integration-test</phase>
            <goals>
              <goal>test</goal>
            </goals>
            <configuration>
              <cucumberArgs>--no-colors</cucumberArgs>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
XML
    end

    it 'should allow to configure plugins in various ways' do
      @project.plugin('org.codehaus.mojo:gwt-maven-plugin', '2.1.0') do |gwt|
        gwt.with({ :extraJvmArgs => "-Xmx512m",
                   :runTarget => "example.html"
                 })
        gwt.execution.goals << ["clean", "compile", "test"]
      end
      @project.plugin("org.mortbay.jetty:jetty-maven-plugin", "7.2.2.v20101205").with({
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
      @project.plugin(:war).with({
                                   :webResources => Maven::Model::NamedArray.new(:resource) do |l|
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
      @project.to_xml.should == <<-XML
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>test</groupId>
  <artifactId>project</artifactId>
  <version>0.0.0</version>
  <build>
    <plugins>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>gwt-maven-plugin</artifactId>
        <version>2.1.0</version>
        <configuration>
          <extraJvmArgs>-Xmx512m</extraJvmArgs>
          <runTarget>example.html</runTarget>
        </configuration>
        <executions>
          <execution>
            <goals>
              <goal>clean</goal>
              <goal>compile</goal>
              <goal>test</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.mortbay.jetty</groupId>
        <artifactId>jetty-maven-plugin</artifactId>
        <version>7.2.2.v20101205</version>
        <configuration>
          <connectors>
		<connector implementation="org.eclipse.jetty.server.nio.SelectChannelConnector">
		  <port>8080</port>
		</connector>
		<connector implementation="org.eclipse.jetty.server.ssl.SslSelectChannelConnector">
		  <port>8443</port>
		  <keystore>${project.basedir}/src/test/resources/server.keystore</keystore>
		  <keyPassword>123456</keyPassword>
		  <password>123456</password>
		</connector>
          </connectors>
        </configuration>
      </plugin>
      <plugin>
        <artifactId>maven-war-plugin</artifactId>
        <configuration>
          <webResources>
            <resource>
              <directory>public</directory>
            </resource>
            <resource>
              <directory>.</directory>
              <includes>
                <include>app/**</include>
                <include>config/**</include>
                <include>lib/**</include>
                <include>vendor/**</include>
                <include>Gemfile</include>
              </includes>
              <targetPath>WEB-INF</targetPath>
            </resource>
            <resource>
              <directory>${gem.path}</directory>
              <targetPath>WEB-INF/gems</targetPath>
            </resource>
          </webResources>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
XML
    end
  end

  describe Maven::Model::Build do

    before :each do
      @build = Maven::Model::Build.new
    end

    it 'should setup an empty build' do
      @build.to_xml.should == ""
    end

    it 'should setup a build with final_name' do
      @build.final_name "final"
      @build.to_xml.should == <<-XML
<build>
  <finalName>final</finalName>
</build>
XML
    end
  end

  describe Maven::Model::Profile do

    before :each do
      @profile = Maven::Model::Profile.new(:test)
    end
    
    it 'should setup an empty profile' do
      @profile.to_xml.should == <<-XML
<profile>
  <id>test</id>
</profile>
XML
    end
    it 'should setup a profile with activation by default' do
      @profile.activation.by_default
      @profile.to_xml.should == <<-XML
<profile>
  <id>test</id>
  <activation>
    <activeByDefault>true</activeByDefault>
  </activation>
</profile>
XML
    end
    it 'should setup a profile with activation by property' do
      @profile.activation.property("rails.env", "test")
      @profile.to_xml.should == <<-XML
<profile>
  <id>test</id>
  <activation>
    <property>
      <name>rails.env</name>
      <value>test</value>
    </property>
  </activation>
</profile>
XML
    end
    it 'should setup a profile with activation by OS family' do
      @profile.activation.os.family "mac"
      @profile.to_xml.should == <<-XML
<profile>
  <id>test</id>
  <activation>
    <os>
      <family>mac</family>
    </os>
  </activation>
</profile>
XML
    end
    it 'should setup a profile with properties' do
      @profile.properties.merge!({  
         "gem.home" => "${project.build.directory}/rubygems-production", 
         "gem.path" => "${project.build.directory}/rubygems-production" 
      })
      @profile.to_xml.should == <<-XML
<profile>
  <id>test</id>
  <properties>
    <gem.home>${project.build.directory}/rubygems-production</gem.home>
    <gem.path>${project.build.directory}/rubygems-production</gem.path>
  </properties>
</profile>
XML
    end
    it 'should setup a profile with plugins' do
      @profile.plugin("org.mortbay.jetty:jetty-maven-plugin", "${jetty.version}")
      @profile.to_xml.should == <<-XML
<profile>
  <id>test</id>
  <build>
    <plugins>
      <plugin>
        <groupId>org.mortbay.jetty</groupId>
        <artifactId>jetty-maven-plugin</artifactId>
        <version>${jetty.version}</version>
      </plugin>
    </plugins>
  </build>
</profile>
XML
    end
    it 'should setup a profile with gem dependency and dependency_management' do
      @profile.gem "cucumber", nil
      @profile.dependency_management.gem "cucumber", "0.9.4"
      @profile.to_xml.should == <<-XML
<profile>
  <id>test</id>
  <dependencies>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>cucumber</artifactId>
      <type>gem</type>
    </dependency>
  </dependencies>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>cucumber</artifactId>
        <version>0.9.4</version>
        <type>gem</type>
      </dependency>
    </dependencies>
  </dependencyManagement>
</profile>
XML
    end
  end
end
