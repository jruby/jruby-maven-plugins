#-*- mode: ruby -*-
Gem::Specification.new do |s|
  s.name = 'deps'
  s.version = '1.0.0'
  s.add_dependency 'slf4r', '>0.4.0'
  s.add_development_dependency 'rspec', '~>2.4.0'
  s.add_development_dependency 'cucumber', ['>= 0.10.0', "< 0.11.1"]
end
