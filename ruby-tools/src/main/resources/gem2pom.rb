require __FILE__.sub(/gem2pom/, 'gem_artifacts')
maven = Maven::LocalRepository.new('', 'rubygems', 'rubygems.org', ARGV[1])

puts maven.to_pomxml(ARGV[0])
