# you need maven 3

# http://maven.apache.org/download.html

mvn rails3:rails -Dapp_path=myapp

cd myapp

mvn rails3:generate -Dgenerator=scaffold -Dargs="account name:string"
mvn rails3:rake -Dtask=db:migrate
mvn rails3:server


# to get away from maven, pack all gems in vendor
mvn ruby:jruby -Djruby.args="target/rubygems/bin/bundle pack"
