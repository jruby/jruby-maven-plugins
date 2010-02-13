# you need maven 3 and java 1.6 !!!!

# http://maven.apache.org/download.html

# get the pom.xml (once the plugin is part of the maven central repository 
# this pom becomes obsolete). the pom.xml also downloads a snapshot of the
# activerecord-jdbc-adapter.gem from my personal repository

wget http://github.com/mkristian/jruby-maven-plugins/raw/master/rails3-maven-plugin/demo/pom.xml

# generate the rails application
mvn rails3:rails -Dapp_path=myapp

# go into the application directory
cd myapp

# scaffold a resource
mvn rails3:generate -Dgenerator=scaffold -Dargs="account name:string"

# migrate the database
mvn rails3:rake -Dtask=db:migrate

# start the server
mvn rails3:server


# to get away from maven, pack all gems in vendor
mvn ruby:jruby -Djruby.args="target/rubygems/bin/bundle pack"

# from that point you know better how to procede . . .
