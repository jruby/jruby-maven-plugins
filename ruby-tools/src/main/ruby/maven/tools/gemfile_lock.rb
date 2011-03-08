module Maven
  module Tools
    class GemfileLock < Hash
     
      class Dependency
        attr_accessor :name, :version, :dependencies
        def initialize(line, deps = {})
          @name = line.sub(/\ .*/,'')
          @version =  line.sub(/.*\(/, '').sub(/\).*/, '').sub(/-java$/, '')
          @dependencies = deps
        end
        
        def add(line)
          dependencies[line.sub(/\ .*/,'')] = line.sub(/.*\(/, '').sub(/\).*/, '')
        end
      end
    
      def initialize(file)
        current = nil
        f = file.is_a?(File) ? file.path: file
        if File.exists? f
          File.readlines(f).each do |line|
            if line =~ /^    [^ ]/
              line.strip!
              current = Dependency.new(line)
              self[current.name] = current
            elsif line =~ /^      [^ ]/
              line.strip!
              current.add(line) if current
            end
          end
        end
      end

      def recurse(result, dep)
        result[dep] = self[dep].version if self[dep] && !result.key?(dep)
        if d = self[dep]
          d.dependencies.each do |name, version|
            unless result.key? name
              result[name] = self[name].nil?? version : self[name].version
              recurse(result, name)
            end
          end
        end
      end

      def dependency_hull(deps = [])
        deps = deps.is_a?(Array) ? deps : [deps]
        result = {}
        deps.each do |dep|
          recurse(result, dep)
        end
        result
      end

      def hull
        dependency_hull(keys)
      end
    end
  end
end

if $0 == __FILE__
  lockfile = Maven::Tools::GemfileLock.new(File.new(ARGV[0] || 'Gemfile.lock'))
  p lockfile
  p lockfile.dependency_hull("rails")
end
