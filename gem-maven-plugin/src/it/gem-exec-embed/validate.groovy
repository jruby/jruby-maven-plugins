failure = false
new File(basedir, 'build.log').eachLine{ 
  failure = failure || (it =~ 'hello world')
//  println "read the following line -> " + it + " " + failure
}


assert failure
