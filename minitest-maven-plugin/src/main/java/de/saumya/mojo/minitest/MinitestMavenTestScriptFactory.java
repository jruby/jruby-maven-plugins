package de.saumya.mojo.minitest;

import de.saumya.mojo.tests.AbstractMavenTestScriptFactory;

public class MinitestMavenTestScriptFactory extends AbstractMavenTestScriptFactory {

    private final boolean useGem;
    private final boolean useBundler;

    public MinitestMavenTestScriptFactory(boolean useGem, boolean useBundler){
        this.useGem = useGem;
        this.useBundler = useBundler;
    }

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
        if (useBundler) {
            builder.append("require 'bundler'\n");
            builder.append("Bundler.require\n");
        }
        if (useGem) {
            builder.append("gem 'minitest'\n");
        }
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
