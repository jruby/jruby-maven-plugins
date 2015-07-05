# jruby maven plugins and extensions

the plugins and extensions are modeled after [jruby-gradle](http://jruby-gradle.github.io/) and uses the old jruby maven plugins under the hood but it needs jruby-1.7.19 or newer (including jruby-9.0.0.0 serie).

even though the plugin depends on the old jruby-maven-plugins it has a different version.

## general command line switches

to see the java/jruby command the plugin is executing use (for example with the verify goal)

```mvn verify -Djruby.verbose```

to quickly pick another jruby version use

```mvn verify -Djruby.version=1.7.20```

## jruby9-exec-maven-plugin

it install the gems and jars from ALL scopes and the plugin sections and can execute ruby. execution can be

* inline ruby: ```-e 'puts JRUBY_VERSION'```
* via command installed via a gem: ```-S rake -T```
* a ruby script: ```my.rb```

see more at [jruby9-exec-maven-plugin](jruby9-exec-maven-plugin)

## jruby9-jar-maven-plugin

it packs all gems and jars from the compile/runtime as well the declared resources into runnable jar. execution will with packed jar

* inline ruby: ```java -jar my.jar -e 'puts JRUBY_VERSION'```
* via command installed via a gem: ```java -jar my.jar -S rake -T```
* a ruby script: ```java -jar my.jar my.rb```

see more at [jruby9-jar-maven-plugin](jruby9-jar-maven-plugin)

## jruby9-jar-extension

it produces the same jar as with jruby9-jar-plugin but the configuration is more compact using a maven extension.

see more at [jruby9-jar-extension](jruby9-jar-extension)

# meta-fu

create an issue if something feels not right

[issues/new](issues/new)

otherwise enjoy :)
