package de.saumya.mojo.cucumber;

import de.saumya.mojo.runit.AbstractMavenTestScriptFactory;

public class CucumberMavenTestScriptFactory extends AbstractMavenTestScriptFactory {
    
    @Override
    protected void getRunnerScript(StringBuilder builder) {
        builder.append("cucumber_report_path = REPORT_PATH + '_tmp'\n");
        builder.append("at_exit do\n");
        builder.append("  # create test like result files\n");
        builder.append("\n");
        builder.append("  require 'rexml/document'\n");
        builder.append("  require 'fileutils'\n");
        builder.append("  FileUtils.mkdir_p(REPORT_PATH)\n");
        builder.append("  tests, errors, failures, skips, time = 0, 0, 0, 0, 0.0\n");
        builder.append("  Dir[File.join(cucumber_report_path, '*xml')].each do |report|\n");
        builder.append("    doc = REXML::Document.new(File.new(report))\n");
        builder.append("    suite = REXML::XPath.first(doc, '//testsuite')\n");
        builder.append("    tests += suite.attributes['tests'].to_i\n");
        builder.append("    errors += suite.attributes['errors'].to_i\n");
        builder.append("    failures += suite.attributes['failures'].to_i\n");
        builder.append("    skips += suite.attributes['skips'].to_i\n");
        builder.append("    time += suite.attributes['time'].to_f\n");
        builder.append("    FileUtils.move(report, File.join(REPORT_PATH, " +
        		"File.basename(report).sub(/\\.xml/, \"-#{JRUBY_VERSION}--#{RUBY_VERSION.sub(/([0-9]\\.[0-9])\\..*$/) { $1 }}.xml\")))\n");
        builder.append("  end\n");
        builder.append("  FileUtils.rm_rf(cucumber_report_path)\n");
        builder.append("  cucumber_summary = File.join(TARGET_DIR, 'cucumber.txt')\n");
        builder.append("  File.open(cucumber_summary, 'w') do |f|\n");
        builder.append("    f.puts \"Finished tests in #{time}s.\"\n");
        builder.append("    f.puts \"#{tests} tests, 0 assertions, #{failures} failures, #{errors} errors, #{skips} skips\"\n");
        builder.append("  end\n");
        builder.append("end\n");
        builder.append("\n");
        builder.append("require 'rubygems'\n");
        builder.append("gem 'cucumber'\n");
        builder.append("argv = ['-f', 'pretty', '-f', 'junit', '-o', cucumber_report_path] + ARGV\n");
        builder.append("ARGV.replace(argv)\n");
        builder.append("load Gem.bin_path('cucumber', 'cucumber')\n");
        builder.append("\n");
    }

    @Override
    protected void getResultsScript(StringBuilder builder) {
//        builder.append("# A little exit code magic\n");
//        builder.append("\n");
//
//        builder.append("if File.new(cucumber_summary).read =~ /, 0 failures/ \n");
//        builder.append("  exit 0\n" );
//        builder.append("else\n");
//        builder.append("  exit 1\n" );
//        builder.append("end\n");
//        builder.append("\n");
    }

    @Override
    protected String getScriptName() {
        return "cucumber-runner.rb";
    }
}