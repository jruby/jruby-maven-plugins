 module Maven
  module Tools
    VERSIONS = { 
      :jetty_plugin => "@jetty.version@",
      :jruby_rack => "@jruby.rack.version@",
      :war_plugin => "@war.version@",
      :jruby_plugins => "@project.version@",
      :jruby_version => "@jruby.version@",
    }.freeze
  end
end
