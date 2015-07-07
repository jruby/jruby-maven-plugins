# jruby jar extension

it packs a ruby application as runnable jar, i.e. all the ruby code and the gems and jars (which ruby loads via require) are packed inside the jar. the jar will include jruby-complete and jruby-mains to execute the ruby application via, i.e.

    java -jar my.jar -S rake

the extension uses the mojos from [../jruby9-jar-maven-plugin](jruby9-jar-maven-plugin)

## general command line switches

to see the java/jruby command the plugin is executing use (for example with the verify goal)

```mvn verify -Djruby.verbose```

to quickly pick another jruby version use

```mvn verify -Djruby.version=1.7.20```

or to display some help

```mvn jruby9-jar:help -Ddetail```
```mvn jruby9-jar:help -Ddetail -Dgoal=jar```

## jruby jar

it installs all the declared gems and jars from the dependencies section as well the plugin dependencies.

the complete pom for the samples below is in [src/it/jrubyJarExample/pom.xml](src/it/jrubyJarExample/pom.xml) and more details on how it can be executed.

the extension is used by declaring the right packaging

    <packaging>jrubyJar</packaging>

the gem-artifacts are coming from the torquebox rubygems proxy

     <repositories>
       <repository>
         <id>rubygems-releases</id>
         <url>http://rubygems-proxy.torquebox.org/releases</url>
       </repository>
     </repositories>

to use these gems within the depenencies of the plugin you need

     <pluginRepositories>
       <pluginRepository>
         <id>rubygems-releases</id>
         <url>http://rubygems-proxy.torquebox.org/releases</url>
       </pluginRepository>
     </pluginRepositories>

the jar and gem artifacts for the JRuby application can be declared in the main dependencies section

    <dependencies>
      <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>1.7.6</version>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>leafy-rack</artifactId>
        <version>0.4.0</version>
        <type>gem</type>
      </dependency>
      <dependency>
        <groupId>rubygems</groupId>
        <artifactId>minitest</artifactId>
        <version>5.7.0</version>
        <type>gem</type>
      </dependency>
    </dependencies>

these artifacts ALL have the default scope which gets packed into the jar.

adding ruby resources to your jar

    <build>
      <resources>
        <resource>
          <directory>${basedir}</directory>
          <includes>
            <include>test.rb</include>
            <include>spec/**</include>
          </includes>
        </resource>
      </resources>

and pick the extension

      <extensions>
        <extension>
          <groupId>de.saumya.mojo</groupId>
          <artifactId>jruby9-jar-extension</artifactId>
          <version>@project.version@</version>
        </extension>
      </extensions>
    </build>
