File.open(__FILE__ + (ENV['GEM_HOME'] ? "-gem" : "") + '.txt', 'w') do |f|
  f.puts ARGV.to_s
  f.puts ENV['GEM_HOME'] if ENV['GEM_HOME']
  f.puts ENV['GEM_PATH'] if ENV['GEM_PATH']
end