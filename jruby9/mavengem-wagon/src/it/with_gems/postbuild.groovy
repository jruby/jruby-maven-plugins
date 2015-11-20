import java.io.*
import org.codehaus.plexus.util.FileUtils

String log = FileUtils.fileRead( new File( basedir, "build.log" ) );

[ "Downloaded: mavengem:https://rubygems.org/rubygems/jar-dependencies/0.2.6/jar-dependencies-0.2.6.pom", "Downloaded: mavengem:https://rubygems.org/rubygems/jar-dependencies/0.2.6/jar-dependencies-0.2.6.gem" ].each {
  
  if ( !log.contains( it ) ) throw new RuntimeException( "log file does not contain '" + it + "'" );

}

true
