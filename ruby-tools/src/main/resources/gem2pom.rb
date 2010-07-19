require __FILE__.sub(/gem2pom/, 'gem_artifacts')
maven = Maven::LocalRepository.new('', 'rubygems', 'rubygems.org')

puts maven.to_pomxml(ARGV[0])
