require File.join(File.dirname(__FILE__), '..', '..', '..', '..', 'main', 'ruby', 'maven', 'model', 'dependencies.rb')

class A < Maven::Model::Tag

  include Maven::Model::Dependencies

end

describe Maven::Model::Dependencies do

  before :each do
    @a = A.new
  end
  
  it 'should have empty dependencies' do
    @a.dependencies.empty?.should be_true
    @a.to_xml.should == <<-XML
<a>
</a>
XML
  end

  it 'should allow gem dependencies with default version' do
    @a.gem 'rubyforge'

    @a.dependencies.empty?.should be_false
    @a.gem?("rubyforge").should be_true
    @a.to_xml.should == <<-XML
<a>
  <dependencies>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>rubyforge</artifactId>
      <version>[0.0.0,)</version>
      <type>gem</type>
    </dependency>
  </dependencies>
</a>
XML
  end  

  it 'should allow gem dependencies with version' do
    @a.gem 'rubyforge', '> 0.1.0', '<=2.0'

    @a.dependencies.empty?.should be_false
    @a.gem?("rubyforge").should be_true
    @a.to_xml.should == <<-XML
<a>
  <dependencies>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>rubyforge</artifactId>
      <version>(0.1.0,2.0]</version>
      <type>gem</type>
    </dependency>
  </dependencies>
</a>
XML
  end

  it 'should allow gem dependencies with exclusions' do
    @a.gem 'rubyforge', ['> 0.1.0', '<=2.0'] do |g|
      g.exclude "rake"
      g.exclude "spork"
    end

    @a.dependencies.empty?.should be_false
    @a.gem?("rubyforge").should be_true
    @a.to_xml.should == <<-XML
<a>
  <dependencies>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>rubyforge</artifactId>
      <version>(0.1.0,2.0]</version>
      <type>gem</type>
      <exclusions>
        <exclusion>
          <groupId>rubygems</groupId>
          <artifactId>rake</artifactId>
        </exclusion>
        <exclusion>
          <groupId>rubygems</groupId>
          <artifactId>spork</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
  </dependencies>
</a>
XML
  end
 
  it 'should allow jar dependencies without version' do
    @a.jar 'org.jruby', 'jruby-core'

    @a.dependencies.empty?.should be_false
    @a.jar?("org.jruby:jruby-core").should be_true
    @a.to_xml.should == <<-XML
<a>
  <dependencies>
    <dependency>
      <groupId>org.jruby</groupId>
      <artifactId>jruby-core</artifactId>
      <type>jar</type>
    </dependency>
  </dependencies>
</a>
XML
  end
  
  it 'should allow jar dependencies with version' do
    @a.jar 'org.jruby:jruby-core', '~> 1.6.0'

    @a.dependencies.empty?.should be_false
    @a.jar?("org.jruby", "jruby-core").should be_true
    @a.to_xml.should == <<-XML
<a>
  <dependencies>
    <dependency>
      <groupId>org.jruby</groupId>
      <artifactId>jruby-core</artifactId>
      <version>[1.6.0,1.6.99999]</version>
      <type>jar</type>
    </dependency>
  </dependencies>
</a>
XML
  end

  it 'should allow jar dependencies with exclusions' do
    @a.jar 'org.jruby', 'jruby-core', '>1.6.0' do |j|
      j.exclusions << "joda:joda-time"
    end

    @a.dependencies.empty?.should be_false
    @a.jar?("org.jruby:jruby-core").should be_true
    @a.to_xml.should == <<-XML
<a>
  <dependencies>
    <dependency>
      <groupId>org.jruby</groupId>
      <artifactId>jruby-core</artifactId>
      <version>(1.6.0,)</version>
      <type>jar</type>
      <exclusions>
        <exclusion>
          <groupId>joda</groupId>
          <artifactId>joda-time</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
  </dependencies>
</a>
XML
  end

  it 'should allow test_jar dependencies with exclusions' do
    @a.test_jar 'org.jruby', 'jruby-stdlib', '= 1.6.0' do |j|
      j.exclusions << ["joda", "joda-time"]
      j.exclusions << "rubyzip2"
    end

    @a.dependencies.empty?.should be_false
    @a.test_jar?("org.jruby:jruby-stdlib").should be_true
    @a.to_xml.should == <<-XML
<a>
  <dependencies>
    <dependency>
      <groupId>org.jruby</groupId>
      <artifactId>jruby-stdlib</artifactId>
      <version>1.6.0</version>
      <type>test-jar</type>
      <exclusions>
        <exclusion>
          <groupId>joda</groupId>
          <artifactId>joda-time</artifactId>
        </exclusion>
        <exclusion>
          <groupId>rubygems</groupId>
          <artifactId>rubyzip2</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
  </dependencies>
</a>
XML
  end

  it 'should allow maven_gem dependencies with exclusions' do
    @a.gem 'mvn:org.jruby:jruby-stdlib', '>= 1.6.0' do |j|
      j.exclusions << ["joda", "joda-time"]
      j.exclusions << "rubyzip2"
    end

    @a.gem "mvn:org.slf4j:slf4j-simple", "1.6.2"

    @a.dependencies.empty?.should be_false
    @a.jar?("org.jruby:jruby-stdlib").should be_false
    @a.maven_gem?("mvn:org.jruby:jruby-stdlib").should be_true
    @a.to_xml.should == <<-XML
<a>
  <dependencies>
    <dependency>
      <groupId>org.jruby</groupId>
      <artifactId>jruby-stdlib</artifactId>
      <version>[1.6.0,)</version>
      <type>maven_gem</type>
      <exclusions>
        <exclusion>
          <groupId>joda</groupId>
          <artifactId>joda-time</artifactId>
        </exclusion>
        <exclusion>
          <groupId>rubygems</groupId>
          <artifactId>rubyzip2</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-simple</artifactId>
      <version>1.6.2</version>
      <type>maven_gem</type>
    </dependency>
  </dependencies>
</a>
XML
  end

  it 'should have only one instance of jar dependency' do
    r1 = @a.jar 'org.jruby:jruby-core', ['=1.6.0']
    r2 = @a.jar 'org.jruby:jruby-core'
    @a.jar?('org.jruby', 'jruby-core').should be_true
    @a.dependencies.size.should == 1
    r1.should == r2
    @a.to_xml.should == <<-XML
<a>
  <dependencies>
    <dependency>
      <groupId>org.jruby</groupId>
      <artifactId>jruby-core</artifactId>
      <version>1.6.0</version>
      <type>jar</type>
    </dependency>
  </dependencies>
</a>
XML
  end
  it 'should have only one instance of gem dependency' do
    r1 = @a.gem 'rubyforge', ['> 0.1.0', '<=2.0']
    r2 = @a.gem 'rubyforge'
    @a.gem?("rubyforge").should be_true
    @a.dependencies.size.should == 1
    r1.should == r2
    @a.to_xml.should == <<-XML
<a>
  <dependencies>
    <dependency>
      <groupId>rubygems</groupId>
      <artifactId>rubyforge</artifactId>
      <version>(0.1.0,2.0]</version>
      <type>gem</type>
    </dependency>
  </dependencies>
</a>
XML
  end
end
