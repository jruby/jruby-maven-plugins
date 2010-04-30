package de.saumya.mojo.rspec;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RSpecScriptFactory extends AbstractScriptFactory {

	public String getScript() throws MalformedURLException {
		StringBuilder builder = new StringBuilder();

		builder.append(getPrologScript());
		builder.append(getClasspathElementsScript());
		builder.append(getPluginClasspathScript());
		builder.append(getConstantsConfigScript());
		builder.append(getRSpecRunnerScript());
		builder.append(getResultsScript());

		return builder.toString();
	}

	private String getConstantsConfigScript() {
		StringBuilder builder = new StringBuilder();

		builder.append("BASE_DIR=%q(" + baseDir + ")\n");
		builder.append("SPEC_DIR=%q(" + sourceDir + ")\n");
		builder.append("REPORT_PATH=%q(" + reportPath + ")\n");
		builder.append("$: << SPEC_DIR\n");

		return builder.toString();
	}

	private String getRSpecRunnerScript() {
		StringBuilder builder = new StringBuilder();

		builder.append("require %q(rubygems)\n");
		builder.append("require %q(spec)\n");
		builder.append("require %q(de/saumya/mojo/rspec/maven_progress_formatter)\n");
		builder.append("options = ::Spec::Runner::OptionParser.parse([\n");
		builder.append("  SPEC_DIR, '-f', \"html:#{REPORT_PATH}\", '-f', 'MavenProgressFormatter', *ARGV\n");
		builder.append("], STDERR, STDOUT)\n");
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

	private String getClasspathElementsScript() throws MalformedURLException {
		List<String> jars = new ArrayList<String>();
		List<String> directories = new ArrayList<String>();

		for (String path : classpathElements) {
			if (path.endsWith(".jar")) {
				jars.add(path);
			} else {
				directories.add(path);
			}
		}

		StringBuilder script = new StringBuilder();

		script.append("MOJO_CLASSPATH={\n");
		script.append("  :directories=>[\n");
		for (String item : directories) {
			script.append("    %q(" + item + "),\n");
		}
		script.append("  ],\n");
		script.append("  :jars=>[\n");
		for (String item : jars) {
			script.append("    %q(" + item + "),\n");
		}
		script.append("  ],\n");
		script.append("}\n");

		script.append("\n\n");

		script.append("MOJO_CLASSPATH[:directories].each do |dir|\n");
		script.append("  $: << dir\n");
		script.append("end\n");

		script.append("MOJO_CLASSPATH[:jars].each do |jar|\n");
		script.append("  require jar\n");
		script.append("end\n");

		return script.toString();

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
