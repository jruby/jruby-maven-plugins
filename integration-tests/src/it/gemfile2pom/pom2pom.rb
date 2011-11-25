pom = File.read('pom.xml').sub(/>war</,'>pom<')
File.open('pom-base.xml', 'w') { |f| f.print(pom) }
File.rename('pom-server.xml', 'pom.xml')
