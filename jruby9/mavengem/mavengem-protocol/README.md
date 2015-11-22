# mavengem protocol

convert a rubygems repository into a maven repository.

## usage via protocol handler

```
de.saumya.mojo.mavengem.Handler.registerMavenGemProtocol()

URL url = new
URL("mavengem:https://rubygems.org/maven/releases/rubygems/rails/maven-metadata.xml");
```

here https://rubygems.org is a rubgems repository the path
"/maven/releases/rubygems/rails/maven-metadata.xml" does not exist
there but the mavengem protocol adds it on the fly using metadata from
the rubygems repository.

for configuration pass in a ```RubygemsFactory```

```
RubygemsFactory factory = ...
Handler.registerMavenGemProtocol(factory)
```

more details belowsee below for the
configuration options.

the protocol handler can be registered only once.

## use the MavenGemURLConnection directly

```
URLConnection con = new
de.saumya.mojo.mavengem.MavenGemURLConnection(new
URL("https://rubygems.org"), "/maven/releases/rubygems/rails/maven-metadata.xml")
```

it is also possible to leave the basepath

```
MavenGemURLConnection(new
URL("https://rubygems.org", "/rubygems/rails/maven-metadata.xml")
```

for configuration again via a ```RubygemsFactory```

```
RubygemsFactory factory = ...
MavenGemURLConnection(factory, new
URL("https://rubygems.org"), "/rubygems/rails/maven-metadata.xml")
```

## configuration

when not using a explicit ```RubygemsFactory``` a default factory is
used.

### configure the default RubygemsFactory

this is done via system properties. the default cache directory is
**$HOME/.mavengem**. to change this to something else use the system
property **mavengem.cachedir**.

to change the mirror setting use the system property
**mavengem.mirror**. setting this means that **ALL** request go
through this mirror whether it is a request to rubygems.org or to any
other rubygems repository.

to have more control over the mirrors, i.e. one mirror per domain, you
need to use an explicit ```RubygemsFactory``` to configure it.

### configure RubygemsFactory

whenever you use an instance of ```RubygemsFactory``` no system
properties are used to configure it, only the constructor arguments.

here you can set the cache directory and the "catch-all-mirror" as
with the default ```RubygemsFactory``` via the system properties. and
you also can pass in a ```Map``` which can map a given rubygems
repository url to mirror.



