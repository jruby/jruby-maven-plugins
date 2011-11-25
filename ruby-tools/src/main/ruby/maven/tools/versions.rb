 module Maven
  module Tools
    VERSIONS = { 
      :jetty_plugin => "@jetty.version@",
      :jruby_rack => "@jruby.rack.version@",
      :war_plugin => "@war.version@",
      :jruby_plugins => "@project.version@",
      :jruby_version => defined?(JRUBY_VERSION) ? JRUBY_VERSION : '1.6.5'
    }.freeze
  end
end
