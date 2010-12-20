package de.saumya.mojo.rspec2;

import java.net.MalformedURLException;
import java.net.URL;

public class RSpecScriptFactory extends AbstractScriptFactory {

    public String getScript() throws MalformedURLException {
        StringBuilder builder = new StringBuilder();

        builder.append(getPrologScript());
        builder.append(getRubygemsSetupScript());

        builder.append(getSystemPropertiesScript());
        builder.append(getPluginClasspathScript());
        builder.append(getTestClasspathSetupScript());

        builder.append(getConstantsConfigScript());

        builder.append(getRSpecRunnerScript());

        builder.append(getResultsScript());

        return builder.toString();
    }

    private String getSystemPropertiesScript() {
        if (systemProperties.keySet().isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        builder.append("# Set up system-properties for running outside of maven\n");
        builder.append("\n");

        for (Object propName : systemProperties.keySet()) {
            String propValue = systemProperties.getProperty(propName.toString());
            builder.append("Java::java.lang::System.setProperty( %q(" + propName.toString() + "), %q(" + propValue + ") )\n");
        }

        builder.append("\n");

        return builder.toString();
    }

    private String getConstantsConfigScript() {
        StringBuilder builder = new StringBuilder();

        builder.append("# Constants used for configuration and execution\n");
        builder.append("\n");

        builder.append("BASE_DIR=%q(" + baseDir + ")\n");
        builder.append("SPEC_DIR=%q(" + sourceDir + ")\n");
        builder.append("REPORT_PATH=%q(" + reportPath + ")\n");
        builder.append("\n");
        builder.append("$: << File.join( BASE_DIR, 'lib' )\n");
        builder.append("$: << SPEC_DIR\n");
        builder.append("\n");

        return builder.toString();
    }

    private String getRSpecRunnerScript() {
        StringBuilder builder = new StringBuilder();

        builder.append("# Use reasonable default arguments or ARGV is passed in from command-line\n");
        builder.append("\n");

        builder.append("run_args = [ SPEC_DIR ]\n");
        builder.append("if ( ! ARGV.empty? )\n");
        builder.append("  run_args = ARGV\n");
        builder.append("end\n");
        builder.append("\n");

        builder.append("require %q(rubygems)\n");
        builder.append("\n");
        builder.append("require %q(rspec)\n");
        builder.append("require %q(rspec/core/formatters/html_formatter)\n");
        builder.append("\n");
        builder.append("require %q(de/saumya/mojo/rspec/multi_formatter)\n");
        builder.append("require %q(de/saumya/mojo/rspec/maven_console_progress_formatter)\n");
        builder.append("\n");
        builder.append("::MultiFormatter.formatters << [ MavenConsoleProgressFormatter, nil ]\n");
        builder.append("::MultiFormatter.formatters << [ RSpec::Core::Formatters::HtmlFormatter, File.open( 'target/rspec-report.html', 'w' ) ] \n");
        builder.append("\n");
        builder.append("::RSpec.configure do |config|\n");
        builder.append("  config.formatter = ::MultiFormatter\n");
        builder.append("end\n");
        builder.append("\n");
        builder.append("::RSpec::Core::Runner.disable_autorun!\n");
        builder.append("::RSpec::Core::Runner.run( run_args, STDERR, STDOUT)\n");
        builder.append("\n");

        return builder.toString();
    }

    private String getResultsScript() {
        StringBuilder builder = new StringBuilder();

        builder.append("# A little magic to report back to maven\n");
        builder.append("\n");

        builder.append("if File.new(REPORT_PATH, 'r').read =~ /, 0 failures/ \n");
        builder.append("  if ( $0 == __FILE__ )\n" );
        builder.append("    exit 0\n" );
        builder.append("  end\n" );
        builder.append("  false\n");
        builder.append("else\n");
        builder.append("  if ( $0 == __FILE__ )\n" );
        builder.append("    exit 1\n" );
        builder.append("  end\n" );
        builder.append("  true\n");
        builder.append("end\n");
        builder.append("\n");

        return builder.toString();
    }

    private String getPrologScript() {
        StringBuilder builder = new StringBuilder();

        builder.append("require %(java)\n");
        builder.append("\n");

        return builder.toString();
    }

    private String getRubygemsSetupScript() {
        if (gemHome == null && gemPath == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        builder.append("# Set up GEM_HOME and GEM_PATH for running outside of maven\n");
        builder.append("\n");

        if (gemHome != null) {
            builder.append("ENV['GEM_HOME']='" + gemHome + "'\n");
        }

        if (gemPath != null) {
            builder.append("ENV['GEM_PATH']='" + gemPath + "'\n");
        }

        builder.append("\n");

        return builder.toString();
    }

    private String getTestClasspathSetupScript() {
        StringBuilder builder = new StringBuilder();

        builder.append("# Set up the classpath for running outside of maven\n");
        builder.append("\n");

        builder.append("def add_classpath_element(element)\n");
        builder.append("  JRuby.runtime.jruby_class_loader.addURL( Java::java.net::URL.new( element ) )\n");
        builder.append("end\n");
        builder.append("\n");

        for (String path : classpathElements) {
            if (!(path.endsWith("jar") || path.endsWith("/"))) {
                path = path + "/";
            }
            builder.append("add_classpath_element(%Q( file://" + path + " ))\n");
        }

        builder.append("\n");

        return builder.toString();
    }

    private String getPluginClasspathScript() {

        String pathToClass = getClass().getName().replaceAll("\\.", "/") + ".class";
        URL here = getClass().getClassLoader().getResource(pathToClass);

        String herePath = here.getPath();

        if (herePath.startsWith("file:")) {
            herePath = herePath.substring(5);
            int bangLoc = herePath.indexOf("!");

            if (bangLoc > 0) {
                herePath = herePath.substring(0, bangLoc);
            }
        }

        if (herePath.endsWith(".jar")) {
            return "require %q(" + herePath + ")\n";
        } else {
            return "$: << %q(" + herePath + ")\n";
        }
    }

    @Override
    protected String getScriptName() {
        return "rspec-runner.rb";
    }

}
