base = java.lang.System.getProperty('maven.rails.basetemplate')
extra = java.lang.System.getProperty('maven.rails.extratemplate')

apply "#{base}" if base

if gwt = java.lang.System.getProperty('maven.rails.gwt')
  
  gem 'resty-generators'

end

apply "#{extra}" if extra
