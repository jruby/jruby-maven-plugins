 module Maven
  module Tools
    VERSIONS = { 
      :jetty_plugin => "@jetty.version@",
      :jruby_rack => "@jruby.rack.version@",
      :war_plugin => "@war.version@",
      :jar_plugin => "@jar.version@",
      :jruby_plugins => "@project.version@",
      :bundler_version => "@bundler.version@",
      :jruby_version => defined?(JRUBY_VERSION) ? JRUBY_VERSION : "@jruby.version@"
    }.freeze
  end
end
