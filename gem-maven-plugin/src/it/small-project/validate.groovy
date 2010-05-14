success_ruby = false
success_java = false
new File(basedir, 'build.log').eachLine{ 
  success_ruby = success_ruby || (it =~ 'hello ruby world')
  success_java = success_java || (it =~ 'Hello Java World!')
  // println "read the following line -> " + it + " " + success
}

assert success_ruby
assert success_java
