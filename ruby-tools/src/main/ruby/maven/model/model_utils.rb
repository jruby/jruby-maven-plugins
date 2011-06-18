module Maven
  module Model
    class Tag

      def self.prepend_tags(*tags)
        _tags(true, *tags)
      end

      def self.tags(*tags)
        _tags(false, *tags)
      end

      def self._tags(prepend, *tags)
        if tags.size == 0
          @tags
        else
          #self.send :attr_accessor, *tags
          tags.each do |tag|
          eval <<-EOF
            def #{tag.to_s}(val = nil)
              @#{tag.to_s} = val if val
              @#{tag.to_s}
            end
            def #{tag.to_s}=(val)
              @#{tag.to_s} = val
            end
EOF
          end
          if self.superclass.respond_to?:tags
            @tags ||= (self.superclass.tags || []).dup
          else
            @tags ||= []
          end
          unless @tags.include? tags[0]
            if prepend
              @tags.replace([tags, @tags].flatten)
            else
              @tags.replace([@tags, tags].flatten)
            end
          end
          @tags
        end
      end

      def _name
        self.class.to_s.downcase.sub(/.*::/, '')
      end

      def initialize(args = {})
        warn "deprecated #{args.inspect}" if args.size > 0
        args.each do |k,v|
          send("#{k}=".to_sym, v)
        end
      end

      def comment(c)
        @comment = c if c
        @comment
      end

      def to_xml(buf = "", indent = "")
        buf << "#{indent}<#{_name}>\n"
        buf << "#{indent}<!--\n#{indent}#{@comment}\n#{indent}-->\n" if @comment
        self.class.tags.each do |var|
          val = instance_variable_get("@#{var}".to_sym)
          var = var.to_s.gsub(/_(.)/) { $1.upcase }
          case val
          when Array
            val.flatten!
            if val.size > 0
              buf << "#{indent}  <#{var}>\n"
              val.each do |v|
                if v.is_a? Tag
                  v.to_xml(buf, indent + "    ")
                else
                  buf << "#{indent}    <#{var.to_s.sub(/s$/, '')}>#{v}</#{var.to_s.sub(/s$/, '')}>\n"
                end
              end
              buf << "#{indent}  </#{var}>\n"
            end
          when Hash
            if val.size > 0
              buf << "#{indent}  <#{var}>\n"
              val.each do |k, v|
                if v.is_a? Tag
                  v.to_xml(buf, indent + "    ")
                else
                  buf << "#{indent}    <#{k}>#{v}</#{k}>\n"
                end
              end
              buf << "#{indent}  </#{var}>\n"
            end
          when Tag
            val.to_xml(buf, indent + "  ")
          else
            #when String
            buf << "#{indent}  <#{var}>#{val}</#{var}>\n" if val
          end
        end
        buf << "#{indent}</#{_name}>\n"
        buf
      end
    end

    class NamedArray < Array
      attr_reader :name
      def initialize(name, &block)
        @name = name.to_s
        if block
          block.call(self)
        end
        self
      end
    end

    class ModelHash < Hash

      def initialize(clazz)
        @clazz = clazz
      end

      def get(key, &block)
        key = key.to_sym if key
        result = self[key]
        if result.nil?
          result = (self[key] = @clazz.new(key))
        end
        if block
          block.call(result)
        end
        result
      end
      alias :new :get
      alias :add :get
      
      def default_model
        @default_model ||= 
          begin
            model = @clazz.new
            self[nil] = model
            model
          end
      end

      def method_missing(method, *args, &block)
        default_model.send(method, *args, &block)
      end
    end
    
    class DeveloperHash < Hash

      def get(*args, &block)
        developer = if args.size == 1 && args[0].is_a?(Developer)
                      args[0] 
                    else 
                      Developer.new(*args)
                    end
        self[developer.id] = developer
        if block
          block.call(developer)
        end
        developer
      end
      alias :new :get
      alias :add :get
    end

    class LicenseHash < Hash

      def get(*args, &block)
        license = if args.size == 1 && args[0].is_a?(License)
                      args[0] 
                    else 
                      License.new(*args)
                    end
        self[license.name] = license
        if block
          block.call(license)
        end
        license
      end
      alias :new :get
      alias :add :get
    end

    class PluginHash < Hash

      def adjust_key(name)
        name = name.to_s
        if (name =~ /\:/).nil?
          if [:jruby, :gem, :rspec, :rake, :rails2, :rails3, :gemify, :cucumber, :runit, :bundler].member? name.to_sym
            "de.saumya.mojo:#{name}-maven-plugin"
          else
            "maven-#{name}-plugin"
          end
        else
          name
        end
      end

      def key?(k)
        super( adjust_key(k).to_sym )
      end

      def get(*args, &block)
        case args.size
        when 3
          name = "#{args[0]}:#{args[1]}"
          version = args[2]
        when 2
          name = args[0].to_s
          version = args[1]
        when 1
          name = args[0].to_s
        else
          raise "need name"
        end

        name = adjust_key(name)
        group_id = name =~ /\:/ ? name.sub(/:.+$/, '') : nil
        artifact_id = name.sub(/^.+:/, '')

        k = "#{group_id}:#{artifact_id}".to_sym
        result = self[k]
        if result.nil?
          result = (self[k] = Plugin.new(group_id, artifact_id, version))
        end
        result.version = version if version
        if block
          block.call(result)
        end
        result
      end
      alias :new :get
      alias :add :get
      
    end

    class ListItems < Tag

      def initialize(name = nil)
        @name = name
      end

      def add(item)
        @items ||= Array.new
        @items << item
      end
      alias :<< :add

      def to_xml(buf = "", indent = "")
        buf << "#{indent}<#{@name}>\n" if @name 
        buf << "#{indent}<!--\n#{indent}#{@comment}\n#{indent}-->\n" if @comment
        @items.each do |i|
          i.to_xml(buf, indent)
        end
        buf << "#{indent}</#{@name}>\n" if @name
      end
      
    end

    class HashTag < Tag

      def initialize(name, args = {})
        @name = name
        @props = args
      end

      def [](key, value)
        @props ||= {}
        @props[key] = value
      end
      
      def to_xml(buf = "", indent = "")
        buf << "#{indent}<#{@name}>\n"
        buf << "#{indent}<!--\n#{indent}#{@comment}\n#{indent}-->\n" if @comment
        map_to_xml(buf, indent, @props)
        buf << "#{indent}</#{@name}>\n"
      end
      
      def map_to_xml(buf, indent, map)
        # sort the hash over the keys
        map.collect { |k,v| [k.to_s, v]}.sort.each do |k,v|
          case v
          when Hash
            buf << "#{indent}  <#{k}>\n"
            map_to_xml(buf, indent + "  ", v)
            buf << "#{indent}  </#{k}>\n"
          when NamedArray
            buf << "#{indent}  <#{k}>\n"
            v.each do|i|
              buf << "#{indent}    <#{v.name}>\n"
              case i
              when Hash
                map_to_xml(buf, indent + "    ", i)
              end
              buf << "#{indent}    </#{v.name}>\n"
            end
            buf << "#{indent}  </#{k}>\n"
          when Array
            buf << "#{indent}  <#{k}>\n"
            singular = k.to_s.sub(/s$/, '')
            v.each do |i|
              buf << "#{indent}    <#{singular}>#{i}</#{singular}>\n"
            end
            buf << "#{indent}  </#{k}>\n"
          when /\n$/
            buf << "#{indent}  <#{k}>#{v}"
            buf << "#{indent}  </#{k}>\n"
          else
            buf << "#{indent}  <#{k}>#{v}</#{k}>\n"
          end
        end
      end
    end
  end
end
