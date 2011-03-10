module Maven
  module Tools
    class PomGenerator
      def read_rails(filename)
        proj = Maven::Tools::RailsProject.new
        proj.load(filename.to_s)
        proj.load(File.join(File.dirname(filename.to_s), 'Mavenfile'))
        proj.add_defaults
        proj.dump_loaded_file_list
        proj.to_xml
      end

      def read_gemfile(filename)
        proj = Maven::Tools::GemProject.new
        proj.load(filename.to_s)
        proj.load(File.join(File.dirname(filename.to_s), 'Mavenfile'))
        proj.add_defaults
        proj.dump_loaded_file_list
        proj.to_xml
      end

      def read_gemspec(filename)
        proj = Maven::Tools::GemProject.new
        proj.load_gemspec(filename.to_s)
        proj.load(File.join(File.dirname(filename.to_s), 'Mavenfile'))
        proj.add_defaults
        proj.dump_loaded_file_list
        proj.to_xml
      end
    end
  end
end

generator = Maven::Tools::PomGenerator.new

if ARGV.size == 2
  puts generator.send("load_#{ARGV[0]}".to_sym, ARGV[1])
else
  generator
end

