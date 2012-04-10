package de.saumya.mojo.cucumber;

import de.saumya.mojo.runit.AbstractMavenTestScriptFactory;

public class CucumberMavenTestScriptFactory extends AbstractMavenTestScriptFactory {
    
    @Override
    protected void getRunnerScript(StringBuilder builder) {
        builder.append("require 'rubygems'\n");
        builder.append("gem 'cucumber'\n");
        builder.append("argv = ['-f', 'pretty', '-f', 'junit', '-o', REPORT_PATH] + ARGV\n");
        builder.append("ARGV.replace(argv)\n");
        builder.append("load Gem.bin_path('cucumber', 'cucumber')\n");
    }

    @Override
    protected String getScriptName() {
        return "cucumber-runner.rb";
    }
}