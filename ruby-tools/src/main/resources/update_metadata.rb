require __FILE__.sub(/update_metadata/, 'gem_artifacts')
maven = Maven::LocalRepository.new('', ARGV[0], 'rubygems.org', ARGV[1])

maven.update_all_metadata(false)
