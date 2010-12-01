package de.saumya.mojo.rspec;

import java.net.MalformedURLException;
import java.net.URL;

public class RSpecScriptFactory extends AbstractScriptFactory {

	public String getScript() throws MalformedURLException {
		StringBuilder builder = new StringBuilder();

		builder.append(getPrologScript());
		builder.append(getSystemPropertiesScript());
		builder.append(getPluginClasspathScript());
		builder.append(getConstantsConfigScript());
		builder.append(getRSpecRunnerScript());
		builder.append(getResultsScript());

		return builder.toString();
	}

	private String getSystemPropertiesScript() {
		StringBuilder builder = new StringBuilder();
		
		for (Object propName : systemProperties.keySet()) {
			String propValue = systemProperties.getProperty(propName.toString());
			builder.append("Java::java.lang::System.setProperty( %q(" + propName.toString() + "), %q(" + propValue + ") )\n" );
		}
		
		return builder.toString();
	}
	private String getConstantsConfigScript() {
		StringBuilder builder = new StringBuilder();

		builder.append("BASE_DIR=%q(" + baseDir + ")\n");
		builder.append("SPEC_DIR=%q(" + sourceDir + ")\n");
		builder.append("REPORT_PATH=%q(" + reportPath + ")\n");
		builder.append("$: << File.join( BASE_DIR, 'lib' )\n");
		builder.append("$: << SPEC_DIR\n");

		return builder.toString();
	}

	private String getRSpecRunnerScript() {
		StringBuilder builder = new StringBuilder();
		
		builder.append( "things = [ SPEC_DIR ]\n");
		builder.append( "if ( ! ARGV.empty? )\n" );
		builder.append( "  things = ARGV\n" );
		builder.append( "end\n" );
		

		builder.append("require %q(rubygems)\n");
		builder.append("require %q(spec)\n");
		builder.append("require %q(de/saumya/mojo/rspec/maven_console_progress_formatter)\n");
		builder.append("require %q(de/saumya/mojo/rspec/maven_surefire_reporter)\n");
		builder.append("options = ::Spec::Runner::OptionParser.parse([\n");
		builder.append("  things,\n");
		builder.append("  '-f', \"html:#{REPORT_PATH}\",\n");
		builder.append("  '-f', 'MavenConsoleProgressFormatter',\n");
		builder.append("  '-f', 'MavenSurefireReporter:target/surefire-reports/',\n");
		builder.append("].flatten, STDERR, STDOUT)\n");
		builder.append("::Spec::Runner::CommandLine.run(options)\n");

		return builder.toString();
	}

	private String getResultsScript() {
		StringBuilder builder = new StringBuilder();

		builder.append("if File.new(REPORT_PATH, 'r').read =~ /, 0 failures/ \n");
		builder.append("  false\n");
		builder.append("else\n");
		builder.append("  true\n");
		builder.append("end\n");

		return builder.toString();
	}

	private String getPrologScript() {
		StringBuilder builder = new StringBuilder();

		builder.append("require %(java)\n");

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
