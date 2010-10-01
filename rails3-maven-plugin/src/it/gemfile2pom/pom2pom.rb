pom = File.read('pom.xml').sub(/>war</,'>pom<')#.sub(/<repositories>(\n|.)*<\/repositories>/, '')
File.open('pom-base.xml', 'w') { |f| f.print(pom) }
File.rename('pom-server.xml', 'pom.xml')
