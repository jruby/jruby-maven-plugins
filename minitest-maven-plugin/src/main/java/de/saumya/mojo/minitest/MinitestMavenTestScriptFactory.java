package de.saumya.mojo.minitest;

import de.saumya.mojo.runit.AbstractMavenTestScriptFactory;

public class MinitestMavenTestScriptFactory extends AbstractMavenTestScriptFactory {
    
    @Override
    protected void getRunnerScript(StringBuilder builder) {
        getTeeClass(builder);
        getAddTestCases(builder);
        getTestRunnerScript(builder);
    }

    @Override
    protected void getResultsScript(StringBuilder builder) {
        // not needed - is already done by test runner
    }

    void getTeeClass(StringBuilder builder){
        builder.append("class Tee < File\n");
        builder.append("  def write(*args)\n");
        builder.append("    super\n" );
        builder.append("    STDOUT.write *args\n" );
        builder.append("  end\n");
        builder.append("  def flush(*args)\n" );
        builder.append("    super\n" );
        builder.append("    STDOUT.flush *args\n");
        builder.append("  end\n" );
        builder.append("end\n");
    }

    void getAddTestCases(StringBuilder builder){
        builder.append("require 'rubygems'\n");
        builder.append("require 'minitest/autorun'\n");
        builder.append("Dir[SOURCE_DIR].each { |f| require f if File.file? f }\n");
    }

    private void getTestRunnerScript(StringBuilder builder) {
        builder.append("MiniTest::Unit.output = Tee.open(REPORT_PATH, 'w')\n");
        builder.append("\n");
   }

    @Override
    protected String getScriptName() {
        return "minitest-runner.rb";
    }

}
