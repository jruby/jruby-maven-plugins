package de.saumya.mojo.rspec;

public class RSpec1ScriptFactory extends AbstractRSpecScriptFactory {


	protected String getRSpecRunnerScript() {
		StringBuilder builder = new StringBuilder();
		
		builder.append( "things = [ SPEC_DIR ]\n");
		builder.append( "if ( ! ARGV.empty? )\n" );
		builder.append( "  things = ARGV\n" );
		builder.append( "end\n" );
		

		builder.append("require %q(rubygems)\n");
		builder.append("require %q(spec)\n");
		builder.append("require %q(de/saumya/mojo/rspec/rspec1/maven_console_progress_formatter)\n");
		builder.append("require %q(de/saumya/mojo/rspec/rspec1/maven_surefire_reporter)\n");
		builder.append("options = ::Spec::Runner::OptionParser.parse([\n");
		builder.append("  things,\n");
		builder.append("  '-f', \"html:#{REPORT_PATH}\",\n");
		builder.append("  '-f', 'MavenConsoleProgressFormatter',\n");
		builder.append("  '-f', \"MavenSurefireReporter:#{TARGET_DIR}/surefire-reports/\",\n");
		builder.append("].flatten, STDERR, STDOUT)\n");
		builder.append("::Spec::Runner::CommandLine.run(options)\n");

		return builder.toString();
	}

}
