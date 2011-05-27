package de.saumya.mojo.runit;

public class Runit19MavenTestScriptFactory extends AbstractRunitMavenTestScriptFactory {

    @Override
    void getTestRunnerScript(StringBuilder builder) {
        builder.append("require 'minitest/autorun'\n");
        builder.append("MiniTest::Unit.output = Tee.open(REPORT_PATH, 'w')\n");
    }

    @Override
    protected String getScriptName() {
        return "minitest-runner.rb";
    }

}
