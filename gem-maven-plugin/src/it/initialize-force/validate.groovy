success = false
new File(basedir, 'build.log').eachLine{ 
  success = success || (it =~ 'Successfully installed rack-1.1.0')
  println "read the following line -> " + it + " " + success
}


assert success
