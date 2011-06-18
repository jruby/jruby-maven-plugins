require 'net/http'
require 'fileutils'
class Crawl

  attr_accessor :target, :base_url

  def load(path)
    target_file = File.join(@target, File.basename(path))
    target_file = "#{target_file}-index.html" if path =~ /\/$/

    p File.basename(target_file)
                                                         
    FileUtils.mkdir_p(File.dirname(target_file))
    File.open(target_file, 'w') do |f|
      f.print Net::HTTP.get(URI.parse("#{base_url}/#{path}"))
    end
  end
end
