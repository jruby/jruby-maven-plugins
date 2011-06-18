db_gem_line = "gem '#{gem_for_database}'"
db_gem_regexp = Regexp::quote(db_gem_line).gsub("'", "['\"]")
jdbc_db = case options[:database]
          when /postgresql/
            "postgres"
          when /mysql2/
            "mysql"
          when /mysql|sqlite3/
            options[:database]
          end

jdbc_gem_line = jdbc_db != 'sqlite3' ? "\n  gem 'jdbc-#{jdbc_db}', :require => false" : <<JDBC
\n
  # As rails --database switch does not support derby, hsqldb, h2 nor mssql
  # as valid values, if you are not using SQLite, comment out the SQLite gem
  # below and uncomment the gem declaration for the adapter you are using.
  # If you are using oracle, db2, sybase, informix or prefer to use the plain
  # JDBC adapter, comment out all the adapter gems below.

  # SQLite JDBC adapter
  gem 'jdbc-sqlite3', :require => false

  # Derby JDBC adapter
  #gem 'activerecord-jdbcderby-adapter'

  # HSQL JDBC adapter
  #gem 'activerecord-jdbchsqldb-adapter'

  # H2 JDBC adapter
  #gem 'activerecord-jdbch2-adapter'

  # SQL Server JDBC adapter
  #gem 'activerecord-jdbcmssql-adapter'
JDBC

if Rails::VERSION::MINOR > 0
  rhino_gem_line = <<RHINO
  # the javascript engine for execjs gem
  gem 'therubyrhino'
RHINO
end

gsub_file "Gemfile", /^#{db_gem_regexp}\w*$/, <<DB
platforms :ruby do
  #{db_gem_line}
end

platforms :jruby do
#{rhino_gem_line}
  gem 'activerecord-jdbc-adapter'#{jdbc_gem_line}
end
DB

# Avoid MySQL2 option for Rails 3
if options[:database] =~ /mysql/
  gsub_file "config/database.yml", /mysql2/, 'mysql'
end

# Add comments to config/database.yml
file = "config/database.yml"

comment = <<COMMENTS
# If you are using derby, hsqldb, h2 or mssql with one of the ActiveRecord
# JDBC adapters, configure your database setting as the example below:
#
#development:
#  adapter: mssql
#  username: <username>
#  password: <password>
#  hostname: localhost
#  database: dummy

# If you are using oracle, db2, sybase, informix or prefer to use the plain
# JDBC adapter, configure your database setting as the example below (requires
# you to download and manually install the database vendor's JDBC driver .jar
# file). See your driver documentation for the apropriate driver class and
# connection string:
#
#development:
#  adapter: jdbc
#  username: <username>
#  password: <password>
#  driver: com.ibm.db2.jcc.DB2Driver
#  url: jdbc:db2://localhost:5021/dummy

COMMENTS

temp = IO.read(file)

open(file, "w") { |f| f << comment << temp }
