jruby maven plugins
===================

[![Build Status](https://buildhive.cloudbees.com/job/torquebox/job/jruby-maven-plugins/badge/icon)](https://buildhive.cloudbees.com/job/torquebox/job/jruby-maven-plugins/)

gem artifacts
-------------

there is maven repository with torquebox.org which delivers gem (only ruby and java platform) from rubygems.org as gem-artifacts. adding this repository to pom.xml (or settings.xml) enables maven to use gem-artifacts like this

    <repositories>
      <repository>
        <id>mavengems</id>
        <url>mavengem:https://rubygems.org</url>
      </repository>
    </repositories>
    . . .
    <dependency>
	  <groupId>rubygems</groupId>
	  <artifactId>compass</artifactId>
	  <version>0.12.2</version>
	  <type>gem</type>
	</dependency>
	
now maven will resolve the transient dependencies of the compass gem and downloads the artifact (includng the gem file) into the local repository.

the next question is how to use those artfacts:

installing gems into you project directory
------------------------------------------

just add the gem-maven-plugin in your pom and execute the 'initialize'. that will install the gem artfacts and its depdencencies into 'target/rubygems'

    <build>
	  <plugins>
        <plugin>
          <groupId>org.jruby.maven</groupId>
          <artifactId>gem-maven-plugin</artifactId>
          <version>${jruby.plugins.version}</version>
          <executions>
            <execution>
              <goals>
                <goal>initialize</goal>
              </goals>
            </execution>
          </executions>
        </plugin>
      </plugins>
	</build>

the will added as test-resource in way that you can use them with ScriptingContainer (from jruby) - see [src/test/java/org/example/javasass/JavaSassTest.java](https://github.com/jruby/jruby-maven-plugins/tree/master/gem-maven-plugin/src/it/include-rubygems-in-test-resources/src/test/java/org/example/javasass/JavaSassTest.java) from integration tests.

example: execute bin/compass from the compass gem
-------------------------------------------------

add the following to you pom
    
    <plugin>
	  <groupId>org.jruby.maven</groupId>
	  <artifactId>gem-maven-plugin</artifactId>
      <version>@project.parent.version@</version>
      <executions>
        <execution>
          <goals>
            <goal>exec</goal>
          </goals>
          <phase>compile</phase>
          <configuration>
            <execArgs>${project.build.directory}/rubygems/bin/compass compile ${basedir}/src/main/webapp/resources/sass</execArgs>
          </configuration>
        </execution>
      </executions>

this will execute **compass** from the compass gem during the *compile* phase. you can further isolate the gems by moving the dependency from root level into the plugin.


    <plugin>
	  <groupId>org.jruby.maven</groupId>
	  <artifactId>gem-maven-plugin</artifactId>
        <version>@project.parent.version@</version>
        <executions>
          <execution>
            <goals>
              <goal>exec</goal>
            </goals>
            <phase>compile</phase>
            <configuration>
              <execArgs>${project.build.directory}/rubygems/bin/compass compile ${basedir}/src/main/webapp/resources/sass</execArgs>
            </configuration>
          </execution>
        </executions>
        <dependencies>
          <dependency>
            <groupId>rubygems</groupId>
            <artifactId>compass</artifactId>
            <version>0.12.2</version>
            <type>gem</type>
          </dependency>
        </dependencies>
      </plugin>
	  
see also [gem-maven-plugin/src/it/execute-compass-with-gems-from-plugin](https://github.com/jruby/jruby-maven-plugins/tree/master/gem-maven-plugin/src/it/execute-compass-with-gems-from-plugin)

more examples
-------------

for more example look into the integration test of the various plugins

* [jruby-maven-plugin/src/it](https://github.com/jruby/jruby-maven-plugins/tree/master/jruby-maven-plugin/src/it)
* [gem-maven-plugin/src/it](https://github.com/jruby/jruby-maven-plugins/tree/master/gem-maven-plugin/src/it)
* . . .

running the intergration tests
------------------------------

```
mvn clean install -Pintegration-test -Pall
```

 
contributing
------------

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Added some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request

meta-fu
-------

enjoy :) 
