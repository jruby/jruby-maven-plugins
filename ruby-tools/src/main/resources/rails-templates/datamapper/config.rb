# original file from https://github.com/datamapper/datamapper.github.com

gsub_file 'config/application.rb', /require 'rails\/all'/ do
<<-RUBY
# Pick the frameworks you want:
require 'action_controller/railtie'
require 'dm-rails/railtie'
# require 'action_mailer/railtie'
# require 'active_resource/railtie'
# require 'rails/test_unit/railtie'
RUBY
end

gsub_file 'config/environments/development.rb', /config.action_mailer.raise_delivery_errors = false/ do
  "# config.action_mailer.raise_delivery_errors = false"
end

gsub_file 'config/environments/test.rb', /config.action_mailer.delivery_method = :test/ do
  "# config.action_mailer.delivery_method = :test"
end
