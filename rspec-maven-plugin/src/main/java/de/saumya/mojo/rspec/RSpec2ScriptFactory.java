package de.saumya.mojo.rspec;


public class RSpec2ScriptFactory extends AbstractRSpecScriptFactory {

    protected String getRSpecRunnerScript() {
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
        builder.append("require %q(de/saumya/mojo/rspec/rspec2/multi_formatter)\n");
        builder.append("require %q(de/saumya/mojo/rspec/rspec2/maven_console_progress_formatter)\n");
        builder.append("require %q(de/saumya/mojo/rspec/rspec2/maven_surefire_reporter)\n");
        builder.append("require %q(de/saumya/mojo/rspec/rspec2/monkey_patch)\n");
        builder.append("\n");
        builder.append("::MultiFormatter.formatters << [ MavenConsoleProgressFormatter, nil ]\n");
        builder.append("::MultiFormatter.formatters << [ MavenSurefireReporter, \"#{TARGET_DIR}\" ] \n");
        builder.append("::MultiFormatter.formatters << [ RSpec::Core::Formatters::HtmlFormatter, File.open( \"#{REPORT_PATH}\", 'w' ) ] \n");
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
}