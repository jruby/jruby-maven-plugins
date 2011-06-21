require 'fileutils'
Dir.glob("rails_app/*").each do |f|
  path = f.sub(/rails_app./, '')
  FileUtils.rm_rf(path)
  FileUtils.mv(f, path)
end
FileUtils.rm_r("rails_app")
FileUtils.rm_r(File.join("target", "jetty"))
pom = File.read('pom.xml').sub(/>war</,'>pom<')
File.open('pom-base.xml', 'w') { |f| f.print(pom) }
File.rename('pom-server.xml', 'pom.xml')
