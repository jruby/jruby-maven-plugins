failure = false
new File(basedir, 'log').eachLine{ 
  failure = failure || (it =~ 'hello kristian')
//  println "read the following line -> " + it + " " + failure
}


assert failure
