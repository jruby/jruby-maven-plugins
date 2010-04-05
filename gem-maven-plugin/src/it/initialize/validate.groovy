success = false
new File(basedir, 'build.log').eachLine{ 
  success = success || (it =~ 'Successfully installed rack-1.0.1')
  println "read the following line -> " + it + " " + success
}


assert success
