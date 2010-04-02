require 'rails_generator/generators/applications/app/template_runner.rb'
require 'rails_generator/simple_logger.rb'

module Rails
  class TemplateRunner

    def loggger
      @logger ||= Generator::SimpleLogger.new
    end

    def log(method, *args)
      logger.send(method.to_sym, args)
    end

    def run_ruby_script(command, log_action = true)
      `java -jar ~/.m2/repository/org/jruby/jruby-complete/1.3.1/jruby-complete-1.3.1.jar #{command}`
    end

    def generate(what, *args)
      log 'generating', what
      argument = args.join(" ") 

      in_root { run_ruby_script("script/generate #{what} #{argument}", false) }
    end

    def run(command, log_action = true)
      log 'executing',  "#{command} from #{Dir.pwd}" if log_action
      if command =~ /^(sudo )?rake/
        `java -jar ~/.m2/repository/org/jruby/jruby-complete/1.3.1/jruby-complete-1.3.1.jar -S #{command}`
      else
        `command`
      end
    end

  end
end
p "fe"

