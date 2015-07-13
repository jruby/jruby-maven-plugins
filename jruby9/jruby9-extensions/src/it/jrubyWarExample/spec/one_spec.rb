require 'jar-dependencies'
describe 'something' do

  it 'runs inside the right environment' do
    expect(Dir.pwd).to eq 'uri:classloader://WEB-INF/classes/'
    expect(__FILE__).to eq 'uri:classloader:/WEB-INF/classes/spec/one_spec.rb'
    expect(Jars.home).to eq 'uri:classloader://WEB-INF/classes/jars'
    expect(Gem.dir).to eq 'uri:classloader://META-INF/jruby.home/lib/ruby/gems/shared'
  end
  
end
