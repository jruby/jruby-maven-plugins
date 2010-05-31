package de.saumya.mojo.rspec;

public class ShellScriptFactory extends AbstractScriptFactory {

    public ShellScriptFactory() {

    }

    public String getScript() {
        StringBuilder builder = new StringBuilder();

        boolean first = true;
        for (String classpathElement : this.classpathElements) {
            if (first) {
                first = false;
                builder.append("CLASSPATH=")
                        .append(classpathElement)
                        .append("\n");
            }
            else {
                builder.append("CLASSPATH=$CLASSPATH:")
                        .append(classpathElement)
                        .append("\n");
            }
        }

        builder.append("export CLASSPATH\n");
        builder.append("this_dir=$(dirname $0)\n");
        builder.append("$JRUBY_HOME/bin/jruby\\\n");
        builder.append("  -J-Dbasedir=").append(baseDir).append("\\\n");
        for (Object propName : systemProperties.keySet()) {
            String propValue = systemProperties.getProperty(propName.toString());
            builder.append("  -J-D")
                    .append(propName)
                    .append("=")
                    .append(propValue)
                    .append("\\\n");
        }
        builder.append("  $this_dir/rspec-runner.rb $*");

        return builder.toString();
    }

    @Override
    protected String getScriptName() {
        return "run-rspecs.sh";
    }

}
