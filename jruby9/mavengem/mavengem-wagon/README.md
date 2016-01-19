# mavengem wagon

extend maven to use mavengem-protocol for configuring a rubygems
repository. this allows to use gem-artifacts as dependencies.

## usage

pom.xml setup

```
  ...
  <repositories>  
    <repository>
      <id>mavengems</id>
      <url>mavengem:http://rubygems.org</url>
    </repository>
  </repositories>
  
  <build>
    <extensions>
      <extension>
        <groupId>de.saumya.mojo</groupId>
        <artifactId>mavengem-wagon</artifactId>
        <version>0.1.0</version>
      </extension>
    </extensions>
  </build>

</project>
```

the same with POM using ruby-DSL

```
repository :id => :mavengems, :url => 'mavengem:http://rubygems.org'

extension 'de.saumya.mojo:mavengem-wagon:0.1.0'
```

the wagon extension allos the use of the **mavengem:** protocol in the
repository url.

## configuration

the configuration happens inside settings.xml (default location is
$HOME/.m2/settings.xml) and uses the **id** from the repository to
allow further configurations.

### cache directory for the mavengem protocol

```
<settings>
  <servers>
    <server>
      <id>mavengems</id>
      <configuration>
        <cachedir>${user.home}/.cachedir</cachedir>
      </configuration>
    </server>
  </servers>
</settings>
```

### username/password authentication

PENDING wating for a new release for the underlying nexus-ruby-tools
library to get this feature working

```
<settings>
  <servers>
    <server>
      <id>mavengems</id>
      <username>my_login</username>
      <password>my_password</password>
    </server>
  </servers>
</settings>
```

### mirror

use a mirror for the configured server

```
<settings>
  <servers>
    <server>
      <id>mavengems</id>
      <configuration>
        <mirror>https://rubygems.org</cachedir>
      </configuration>
    </server>
  </servers>
</settings>
```

the usename and password in a configuration with mirror will be used
for the mirror:

```
<settings>
  <servers>
    <server>
      <id>mavengems</id>
      <username>my_login</username>
      <password>my_password</password>
      <configuration>
        <mirror>https://rubygems.org</cachedir>
      </configuration>
    </server>
  </servers>
</settings>
```
