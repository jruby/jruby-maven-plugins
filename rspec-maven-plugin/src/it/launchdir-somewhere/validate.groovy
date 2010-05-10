failure = false
new File(basedir, 'build.log').eachLine{ 
  failure = failure || (it =~ 'TOTAL: 1 passing; 0 failing; 0 pending')
//  println "read the following line -> " + it + " " + failure
}


assert failure
