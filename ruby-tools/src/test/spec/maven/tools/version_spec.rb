require 'maven/tools/versions'
require 'maven/tools/version'

describe Maven::Tools::VERSION do

  it 'should match the plugins version' do
    v = PLUGINS_VERSION if defined? PLUGINS_VERSION 
    v ||= Maven::Tools::VERSIONS[:jruby_plugins]
    Maven::Tools::VERSION.should == v.sub(/-SNAPSHOT/, '')
  end

end
