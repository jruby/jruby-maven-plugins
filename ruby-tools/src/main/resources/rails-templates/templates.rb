base = java.lang.System.getProperty('maven.rails.basetemplate')
extra = java.lang.System.getProperty('maven.rails.extratemplate')

apply "#{base}" if base
apply "#{extra}" if extra

if gwt = java.lang.System.getProperty('maven.rails.gwt')
  
  gem 'resty-generators'

end
