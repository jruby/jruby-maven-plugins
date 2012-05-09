require 'maven/tools/gem_project'

# keep plugins version for verions test
PLUGINS_VERSION = Maven::Tools::VERSIONS[:jruby_plugins]

Maven::Tools::VERSIONS = { 
      :jetty_plugin => "@jetty.version@",
      :jruby_rack => "@jruby.rack.version@",
      :war_plugin => "@war.version@",
      :jar_plugin => "@jar.version@",
      :jruby_plugins => "@project.version@",
      :jruby_version => defined?(JRUBY_VERSION) ? JRUBY_VERSION : "@jruby.version@"
}

describe Maven::Tools::GemProject do

  before :each do
    @project = Maven::Tools::GemProject.new("test")
  end
  
  it 'should setup an empty gem project' do
    @project.to_xml.should == <<-XML
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>rubygems</groupId>
  <artifactId>test</artifactId>
  <version>0.0.0</version>
  <packaging>gem</packaging>
</project>
XML
  end

  describe "Jarfile" do

    it 'should load Jarfile without lockfile' do
      @project.load_jarfile(File.join(File.dirname(__FILE__), 'Jarfile.without'))
      @project.to_xml.should == <<-XML
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>rubygems</groupId>
  <artifactId>test</artifactId>
  <version>0.0.0</version>
  <packaging>gem</packaging>
  <dependencies>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-simple</artifactId>
      <version>[1.5.6,)</version>
      <type>jar</type>
    </dependency>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-default</artifactId>
      <version>[0,)</version>
      <type>pom</type>
    </dependency>
  </dependencies>
</project>
XML
    end

    it 'should load Jarfile with lockfile' do
      @project.load_jarfile(File.join(File.dirname(__FILE__), 'Jarfile.with'))
      @project.to_xml.should == <<-XML
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>rubygems</groupId>
  <artifactId>test</artifactId>
  <version>0.0.0</version>
  <packaging>gem</packaging>
  <dependencies>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-simple</artifactId>
      <version>1.5.6</version>
      <type>jar</type>
    </dependency>
  </dependencies>
</project>
XML
    end
  end

  describe "Gemfile" do

    it 'should load Gemfile with minimal gemspec' do
      @project.load_gemfile(File.join(File.dirname(__FILE__), 'Gemfile.minimal'))
      @project.to_xml.should == <<-XML
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>rubygems</groupId>
  <artifactId>minimal</artifactId>
  <version>1.0.0</version>
  <name><![CDATA[minimal - gem]]></name>
  <packaging>gem</packaging>
</project>
XML
    end

    it 'should load Gemfile with "source", "path" and "platform"' do
      @project.load_gemfile(File.join(File.dirname(__FILE__), 'Gemfile.ignored'))
      @project.to_xml.should == <<-XML
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>rubygems</groupId>
  <artifactId>test</artifactId>
  <version>0.0.0</version>
  <packaging>gem</packaging>
  <dependencies>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>ixtlan-core</artifactId>
      <version>[0.0.0,)</version>
      <type>gem</type>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>de.saumya.mojo</groupId>
        <artifactId>bundler-maven-plugin</artifactId>
        <dependencies>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>ixtlan-core</artifactId>
            <version>[0.0.0,)</version>
            <type>gem</type>
          </dependency>
        </dependencies>
      </plugin>
    </plugins>
  </build>
</project>
XML
    end

    it 'should load Gemfile with simple gems"' do
      @project.load_gemfile(File.join(File.dirname(__FILE__), 'Gemfile.gems'))
      @project.to_xml.should == <<-XML
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>rubygems</groupId>
  <artifactId>test</artifactId>
  <version>0.0.0</version>
  <packaging>gem</packaging>
  <dependencies>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>ixtlan-core</artifactId>
      <version>[0.0.0,)</version>
      <type>gem</type>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>de.saumya.mojo</groupId>
        <artifactId>bundler-maven-plugin</artifactId>
        <dependencies>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>ixtlan-core</artifactId>
            <version>[0.0.0,)</version>
            <type>gem</type>
          </dependency>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>dm-sqlite3-adapter</artifactId>
            <version>[0.10.0,0.10.99999]</version>
            <type>gem</type>
          </dependency>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>dm-mysql-adapter</artifactId>
            <version>[0.10.0,0.10.3)</version>
            <type>gem</type>
          </dependency>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>dm-hsqldb-adapter</artifactId>
            <version>[0.10.0,0.10.0.0.0.0.0.1)</version>
            <type>gem</type>
          </dependency>
        </dependencies>
      </plugin>
    </plugins>
  </build>
  <profiles>
    <profile>
      <id>test</id>
      <dependencies>
        <dependency>
          <groupId>rubygems</groupId>
          <artifactId>dm-sqlite3-adapter</artifactId>
          <version>[0.10.0,0.10.99999]</version>
          <type>gem</type>
        </dependency>
      </dependencies>
    </profile>
    <profile>
      <id>development</id>
      <dependencies>
        <dependency>
          <groupId>rubygems</groupId>
          <artifactId>dm-mysql-adapter</artifactId>
          <version>[0.10.0,0.10.3)</version>
          <type>gem</type>
        </dependency>
        <dependency>
          <groupId>rubygems</groupId>
          <artifactId>dm-hsqldb-adapter</artifactId>
          <version>[0.10.0,0.10.0.0.0.0.0.1)</version>
          <type>gem</type>
        </dependency>
      </dependencies>
    </profile>
    <profile>
      <id>production</id>
      <dependencies>
        <dependency>
          <groupId>rubygems</groupId>
          <artifactId>dm-mysql-adapter</artifactId>
          <version>[0.10.0,0.10.3)</version>
          <type>gem</type>
        </dependency>
      </dependencies>
    </profile>
  </profiles>
</project>
XML
    end

    it 'should load Gemfile with grouped gems and added defaults"' do
      @project.load_gemfile(File.join(File.dirname(__FILE__), 'Gemfile.groups'))
      @project.name "test"
      @project.add_defaults
      @project.to_xml.should == <<-XML
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>rubygems</groupId>
  <artifactId>test</artifactId>
  <version>0.0.0</version>
  <name><![CDATA[test]]></name>
  <packaging>gem</packaging>
  <repositories>
    <repository>
      <id>rubygems-releases</id>
      <url>http://rubygems-proxy.torquebox.org/releases</url>
    </repository>
    <repository>
      <id>rubygems-prereleases</id>
      <url>http://rubygems-proxy.torquebox.org/prereleases</url>
      <releases>
        <enabled>false</enabled>
      </releases>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
    </repository>
  </repositories>
  <pluginRepositories>
    <pluginRepository>
      <id>rubygems-releases</id>
      <url>http://rubygems-proxy.torquebox.org/releases</url>
    </pluginRepository>
    <pluginRepository>
      <id>rubygems-prereleases</id>
      <url>http://rubygems-proxy.torquebox.org/prereleases</url>
      <releases>
        <enabled>false</enabled>
      </releases>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
    </pluginRepository>
  </pluginRepositories>
  <dependencies>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>bundler</artifactId>
      <version>[0.0.0,)</version>
      <type>gem</type>
    </dependency>
  </dependencies>
  <properties>
    <gem.home>${project.build.directory}/rubygems</gem.home>
    <gem.path>${project.build.directory}/rubygems</gem.path>
    <jruby.plugins.version>@project.version@</jruby.plugins.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
  <build>
    <plugins>
      <plugin>
        <groupId>de.saumya.mojo</groupId>
        <artifactId>bundler-maven-plugin</artifactId>
        <version>${jruby.plugins.version}</version>
        <executions>
          <execution>
            <goals>
              <goal>install</goal>
            </goals>
          </execution>
        </executions>
        <dependencies>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>ixtlan-core</artifactId>
            <version>[0.0.0,)</version>
            <type>gem</type>
          </dependency>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>dm-sqlite-adapter</artifactId>
            <version>[1.0.0,1.0.99999]</version>
            <type>gem</type>
          </dependency>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>dm-mysql-adapter</artifactId>
            <version>[1.0.0,1.0.3)</version>
            <type>gem</type>
          </dependency>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>dm-postgres-adapter</artifactId>
            <version>[0,1.0.0]</version>
            <type>gem</type>
          </dependency>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>bundler</artifactId>
            <version>[0.0.0,)</version>
            <type>gem</type>
          </dependency>
        </dependencies>
      </plugin>
      <plugin>
        <groupId>de.saumya.mojo</groupId>
        <artifactId>gem-maven-plugin</artifactId>
        <version>${jruby.plugins.version}</version>
        <extensions>true</extensions>
      </plugin>
    </plugins>
  </build>
  <profiles>
    <profile>
      <id>test</id>
      <dependencies>
        <dependency>
          <groupId>rubygems</groupId>
          <artifactId>ixtlan-core</artifactId>
          <version>[0.0.0,)</version>
          <type>gem</type>
        </dependency>
      </dependencies>
    </profile>
    <profile>
      <id>production</id>
      <dependencies>
        <dependency>
          <groupId>rubygems</groupId>
          <artifactId>ixtlan-core</artifactId>
          <version>[0.0.0,)</version>
          <type>gem</type>
        </dependency>
        <dependency>
          <groupId>rubygems</groupId>
          <artifactId>dm-mysql-adapter</artifactId>
          <version>[1.0.0,1.0.3)</version>
          <type>gem</type>
        </dependency>
        <dependency>
          <groupId>rubygems</groupId>
          <artifactId>dm-postgres-adapter</artifactId>
          <version>[0,1.0.0]</version>
          <type>gem</type>
        </dependency>
      </dependencies>
    </profile>
    <profile>
      <id>development</id>
      <dependencies>
        <dependency>
          <groupId>rubygems</groupId>
          <artifactId>dm-sqlite-adapter</artifactId>
          <version>[1.0.0,1.0.99999]</version>
          <type>gem</type>
        </dependency>
      </dependencies>
    </profile>
  </profiles>
</project>
XML
    end

    it 'should load Gemfile with grouped gems and lock file"' do
      @project.load_gemfile(File.join(File.dirname(__FILE__), 'Gemfile.lockfile'))
      @project.name "test"
      @project.to_xml.should == <<-XML
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>rubygems</groupId>
  <artifactId>test</artifactId>
  <version>0.0.0</version>
  <name><![CDATA[test]]></name>
  <packaging>gem</packaging>
  <dependencies>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>bundler</artifactId>
      <version>[0.0.0,)</version>
      <type>gem</type>
    </dependency>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>ixtlan-core</artifactId>
      <type>gem</type>
    </dependency>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>slf4r</artifactId>
      <type>gem</type>
    </dependency>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>dm-sqlite-adapter</artifactId>
      <type>gem</type>
    </dependency>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>dm-do-adapter</artifactId>
      <type>gem</type>
    </dependency>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>data_objects</artifactId>
      <type>gem</type>
    </dependency>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>addressable</artifactId>
      <type>gem</type>
    </dependency>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>dm-core</artifactId>
      <type>gem</type>
    </dependency>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>extlib</artifactId>
      <type>gem</type>
    </dependency>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>do_sqlite3</artifactId>
      <type>gem</type>
    </dependency>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>do_jdbc</artifactId>
      <type>gem</type>
    </dependency>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>jdbc-sqlite3</artifactId>
      <type>gem</type>
    </dependency>
  </dependencies>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>bundler</artifactId>
        <version>[0,)</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>addressable</artifactId>
        <version>2.2.4</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>data_objects</artifactId>
        <version>0.10.3</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>dm-core</artifactId>
        <version>1.0.2</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>extlib</artifactId>
        <version>0.9.15</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>dm-do-adapter</artifactId>
        <version>1.0.2</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>dm-mysql-adapter</artifactId>
        <version>1.0.2</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>do_mysql</artifactId>
        <version>0.10.3</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>do_jdbc</artifactId>
        <version>0.10.3</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>jdbc-mysql</artifactId>
        <version>5.0.4</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>dm-postgres-adapter</artifactId>
        <version>1.0.0</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>do_postgres</artifactId>
        <version>0.10.3</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>jdbc-postgres</artifactId>
        <version>8.4.702</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>dm-sqlite-adapter</artifactId>
        <version>1.0.2</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>do_sqlite3</artifactId>
        <version>0.10.3</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>jdbc-sqlite3</artifactId>
        <version>3.6.14.2.056</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>ixtlan-core</artifactId>
        <version>0.1.1</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>slf4r</artifactId>
        <version>0.4.2</version>
        <type>gem</type>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <build>
    <plugins>
      <plugin>
        <groupId>de.saumya.mojo</groupId>
        <artifactId>bundler-maven-plugin</artifactId>
        <dependencies>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>bundler</artifactId>
            <version>[0.0.0,)</version>
            <type>gem</type>
          </dependency>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>ixtlan-core</artifactId>
            <version>[0.0.0,)</version>
            <type>gem</type>
          </dependency>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>dm-sqlite-adapter</artifactId>
            <version>[1.0.0,1.0.99999]</version>
            <type>gem</type>
          </dependency>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>dm-mysql-adapter</artifactId>
            <version>1.0.2</version>
            <type>gem</type>
          </dependency>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>do_mysql</artifactId>
            <version>0.10.3</version>
            <type>gem</type>
          </dependency>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>jdbc-mysql</artifactId>
            <version>5.0.4</version>
            <type>gem</type>
          </dependency>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>dm-postgres-adapter</artifactId>
            <version>1.0.0</version>
            <type>gem</type>
          </dependency>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>do_postgres</artifactId>
            <version>0.10.3</version>
            <type>gem</type>
          </dependency>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>jdbc-postgres</artifactId>
            <version>8.4.702</version>
            <type>gem</type>
          </dependency>
        </dependencies>
      </plugin>
    </plugins>
  </build>
  <profiles>
    <profile>
      <id>production</id>
      <dependencies>
        <dependency>
          <groupId>rubygems</groupId>
          <artifactId>dm-mysql-adapter</artifactId>
          <type>gem</type>
        </dependency>
        <dependency>
          <groupId>rubygems</groupId>
          <artifactId>do_mysql</artifactId>
          <type>gem</type>
        </dependency>
        <dependency>
          <groupId>rubygems</groupId>
          <artifactId>jdbc-mysql</artifactId>
          <type>gem</type>
        </dependency>
        <dependency>
          <groupId>rubygems</groupId>
          <artifactId>dm-postgres-adapter</artifactId>
          <type>gem</type>
        </dependency>
        <dependency>
          <groupId>rubygems</groupId>
          <artifactId>do_postgres</artifactId>
          <type>gem</type>
        </dependency>
        <dependency>
          <groupId>rubygems</groupId>
          <artifactId>jdbc-postgres</artifactId>
          <type>gem</type>
        </dependency>
      </dependencies>
    </profile>
  </profiles>
</project>
XML
    end
  end

  describe "gemspec" do

    it 'should load minimal gemspec' do
      @project.load_gemspec(File.join(File.dirname(__FILE__), 'minimal.gemspec'))
      @project.to_xml.should == <<-XML
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>rubygems</groupId>
  <artifactId>minimal</artifactId>
  <version>1.0.0</version>
  <name><![CDATA[minimal - gem]]></name>
  <packaging>gem</packaging>
  <build>
    <plugins>
      <plugin>
        <groupId>de.saumya.mojo</groupId>
        <artifactId>gem-maven-plugin</artifactId>
        <configuration>
          <gemspec>/home/kristian/projects/active/maven/jruby-maven-plugins/ruby-tools/src/test/spec/maven/tools/minimal.gemspec</gemspec>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
XML
    end

    it 'should load gemspec without dependencies' do
      @project.load_gemspec(File.join(File.dirname(__FILE__), 'no-deps.gemspec'))
      @project.to_xml.should == <<-XML
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>rubygems</groupId>
  <artifactId>no-deps</artifactId>
  <version>1.0.0</version>
  <name><![CDATA[no dependencies project]]></name>
  <packaging>gem</packaging>
  <description><![CDATA[a no dependencies project to demonstrat how gemspec2pom works]]></description>
  <url>http://example.com</url>
  <developers>
    <developer>
      <id>k_at_example_dot_com</id>
      <name>Krysh Sample</name>
      <email>k@example.com</email>
    </developer>
  </developers>
  <licenses>
    <license>
      <name>AGPL</name>
      <url>./AGPL.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <build>
    <plugins>
      <plugin>
        <groupId>de.saumya.mojo</groupId>
        <artifactId>gem-maven-plugin</artifactId>
        <configuration>
          <autorequire>my</autorequire>
          <bindir>mybin</bindir>
          <defaultExecutable>myexe</defaultExecutable>
          <executables>hello</executables>
          <extensions>myext</extensions>
          <extraRdocFiles>README.txt</extraRdocFiles>
          <files>AGPL.txt,README.txt,test/first_test.rb,mybin/hello,myext,lib/first.rb,spec/first_spec.rb,features/first.feature</files>
          <gemspec>/home/kristian/projects/active/maven/jruby-maven-plugins/ruby-tools/src/test/spec/maven/tools/no-deps.gemspec</gemspec>
          <platform>java</platform>
          <postInstallMessage><![CDATA[be happy]]></postInstallMessage>
          <rdocOptions>--main,README.txt</rdocOptions>
          <requirePaths>mylib</requirePaths>
          <requiredRubyVersion><![CDATA[= 1.8.7]]></requiredRubyVersion>
          <requiredRubygemsVersion><![CDATA[= 1.4.2]]></requiredRubygemsVersion>
          <rubyforgeProject>myproject</rubyforgeProject>
          <testFiles>test/first_test.rb</testFiles>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
XML
    end

    it 'should load gemspec with dependencies' do
      @project.load_gemspec(File.join(File.dirname(__FILE__), 'deps.gemspec'))
      @project.to_xml.should == <<-XML
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>rubygems</groupId>
  <artifactId>deps</artifactId>
  <version>1.0.0</version>
  <name><![CDATA[deps - gem]]></name>
  <packaging>gem</packaging>
  <dependencies>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>slf4r</artifactId>
      <version>(0.4.0,)</version>
      <type>gem</type>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>rspec</artifactId>
      <version>[2.4.0,2.4.99999]</version>
      <type>gem</type>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>cucumber</artifactId>
      <version>[0.10.0,0.11.1)</version>
      <type>gem</type>
      <scope>test</scope>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>de.saumya.mojo</groupId>
        <artifactId>gem-maven-plugin</artifactId>
        <configuration>
          <gemspec>/home/kristian/projects/active/maven/jruby-maven-plugins/ruby-tools/src/test/spec/maven/tools/deps.gemspec</gemspec>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
XML
    end

    it 'should load minimal gemspec with applied defaults' do
      @project.load_gemspec(File.join(File.dirname(__FILE__), 'minimal.gemspec'))
      @project.add_defaults
      @project.to_xml.should == <<-XML
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>rubygems</groupId>
  <artifactId>minimal</artifactId>
  <version>1.0.0</version>
  <name><![CDATA[minimal - gem]]></name>
  <packaging>gem</packaging>
  <repositories>
    <repository>
      <id>rubygems-releases</id>
      <url>http://rubygems-proxy.torquebox.org/releases</url>
    </repository>
    <repository>
      <id>rubygems-prereleases</id>
      <url>http://rubygems-proxy.torquebox.org/prereleases</url>
      <releases>
        <enabled>false</enabled>
      </releases>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
    </repository>
  </repositories>
  <properties>
    <gem.home>${project.build.directory}/rubygems</gem.home>
    <gem.path>${project.build.directory}/rubygems</gem.path>
    <jruby.plugins.version>@project.version@</jruby.plugins.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
  <build>
    <plugins>
      <plugin>
        <groupId>de.saumya.mojo</groupId>
        <artifactId>gem-maven-plugin</artifactId>
        <version>${jruby.plugins.version}</version>
        <extensions>true</extensions>
        <configuration>
          <gemspec>/home/kristian/projects/active/maven/jruby-maven-plugins/ruby-tools/src/test/spec/maven/tools/minimal.gemspec</gemspec>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
XML
    end
  end

end
