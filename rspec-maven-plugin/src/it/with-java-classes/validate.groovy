failure = false
new File(basedir, 'build.log').eachLine{ 
  failure = failure || (it =~ 'TOTAL: 1 passing; 0 failing; 0 pending')
  failure = failure || (it =~ 'logging something very important')
//  println "read the following line -> " + it + " " + failure
}


assert failure
