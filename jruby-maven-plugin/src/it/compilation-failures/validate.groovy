failure = false
new File(basedir, 'build.log').eachLine{ 
  failure = failure || (it =~ 'Failure during compilation')
  println "read the following line -> " + it + " " + failure
}


assert failure
