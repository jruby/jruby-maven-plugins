package de.saumya.mojo.rspec;

import de.saumya.mojo.tests.AbstractMavenTestScriptFactory;

public class RSpecMavenTestScriptFactory extends AbstractMavenTestScriptFactory  {

    @Override
    protected void getRunnerScript(StringBuilder builder) {
        builder.append("# Use reasonable default arguments or ARGV is passed in from command-line\n");
        builder.append("\n");

        builder.append(getPluginClasspathScript());

        builder.append("\n");
        builder.append("run_args = [ SOURCE_DIR ]\n");
        builder.append("if ( ! ARGV.empty? )\n");
        builder.append("  run_args = ARGV\n");
        builder.append("end\n");
        builder.append("\n");
    
        builder.append("require %q(rubygems)\n");
        builder.append("\n");
        builder.append("require %q(rspec)\n");
        builder.append("require %q(rspec/core/formatters/html_formatter)\n");
        builder.append("\n");

        builder.append("require %q(de/saumya/mojo/rspec/rspec3/maven_surefire_reporter)\n");
        builder.append("::RSpec.configure do |config|\n");
        builder.append("  config.add_formatter RSpec::Core::Formatters::ProgressFormatter\n");
        builder.append("  config.add_formatter RSpec::Core::Formatters::HtmlFormatter, File.open( \"#{REPORT_PATH}\", 'w' )\n");
        builder.append("  config.add_formatter MavenSurefireReporter\n");
        builder.append("end\n");
        builder.append("\n");
        builder.append("::RSpec::Core::Runner.disable_autorun!\n");
        builder.append("RESULT = ::RSpec::Core::Runner.run( run_args, STDERR, STDOUT)\n");
        builder.append("\n");
    }

    @Override
    protected String getScriptName() {
        return "rspec-runner.rb";
    }

}
