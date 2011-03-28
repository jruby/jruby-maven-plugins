 module Maven
  module Tools
    VERSIONS = { 
      :jetty_plugin => "7.2.2.v20101205",
      :jruby_rack => "1.0.7",
      :war_plugin => "2.1.1",
      :jruby_plugins => "@project.version@",
      :jruby_version => "@jruby.version@",
    }.freeze
  end
end
