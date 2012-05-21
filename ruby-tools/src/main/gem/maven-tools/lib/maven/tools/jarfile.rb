require File.join(File.dirname(__FILE__), 'coordinate.rb')
module Maven
  module Tools

    class Jarfile
      include Coordinate

      def initialize(file = 'Jarfile')
        @file = file
        @lockfile = file + ".lock"
      end

      def mtime
        File.mtime(@file)
      end

      def exists?
        File.exists?(@file)
      end

      def mtime_lock
        File.mtime(@lockfile)
      end

      def exists_lock?
        File.exists?(@lockfile)
      end

      def load_lockfile
        _locked = []
        if exists_lock?
          File.read(@lockfile).each_line do |line|
            line.strip!
            if line.size > 0 && !(line =~ /^\s*#/)
              _locked << line
            end
          end
        end
        _locked
      end

      def locked
        @locked ||= load_lockfile
      end

      def locked?(coordinate)
        coord = coordinate.sub(/^([^:]+:[^:]+):.+/) { $1 }
        locked.detect { |l| l.sub(/^([^:]+:[^:]+):.+/) { $1 } == coord } != nil
      end

      def populate_unlocked(container)
        if File.exists?(@file)
          File.read(@file).each_line do |line| 
            if coord = to_coordinate(line)
              unless locked?(coord)
                container.add_artifact(coord)
              end
            elsif line =~ /^\s*(repository|source)\s/
              name, url = line.sub(/.*(repository|source)\s+/, '').gsub(/['":]/,'').split(/,/)
              url = name unless url
              container.add_repository(name, url)
            end
          end
        end
      end

      def populate_locked(container)
        locked.each { |l| container.add_artifact(l) }
      end

      def generate_lockfile(dependency_coordinates)
        if dependency_coordinates.empty?
          FileUtils.rm_f(@lockfile) if exists_lock?
        else
          File.open(@lockfile, 'w') do |f|
            dependency_coordinates.each do |d|
              f.puts d.to_s
            end
          end
        end
      end
    end

  end
end
