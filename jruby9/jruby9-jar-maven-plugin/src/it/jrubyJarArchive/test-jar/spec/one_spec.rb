require 'jar-dependencies'
describe 'something' do

  it 'runs inside the right environment' do
    expect(Dir.pwd).to eq 'uri:classloader:/'
    expect(__FILE__).to eq (ENV_JAVA['basedir'].gsub('\\', '/') + '/spec/one_spec.rb')
    expect(Jars.home).to eq 'uri:classloader:/jars'
    expect(Gem.dir).to eq 'uri:classloader:/'
  end
  
end
