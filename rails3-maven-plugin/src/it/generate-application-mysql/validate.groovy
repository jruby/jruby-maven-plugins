success1 = false
success2 = false
success3 = false
new File(basedir, 'target/app/Gemfile').eachLine{ 
  success1 = success1 || (it =~ '^gem .mysql.')
  success2 = success2 || (it =~ '^gem "activerecord-jdbc-adapter"')
  success3 = success3 || (it =~ '^gem "jdbc-mysql"')
  println "read the following line -> " + it + " " + success1 + success2 + success3
}

success_jdbc = false
success_version = false
new File(basedir, 'target/app/pom.xml').eachLine{ 
  success_jdbc = success_jdbc || (it =~ '<artifactId>jdbc-mysql</artifactId>')
  success_version = success_version || (it =~ '<version>3.0.0.beta[3-9]</version>')
}

assert success1
assert success2
assert success3
assert success_jdbc
assert success_version
