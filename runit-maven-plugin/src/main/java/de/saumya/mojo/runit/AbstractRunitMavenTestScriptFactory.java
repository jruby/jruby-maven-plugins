package de.saumya.mojo.runit;

import de.saumya.mojo.tests.AbstractMavenTestScriptFactory;

public abstract class AbstractRunitMavenTestScriptFactory extends AbstractMavenTestScriptFactory {

    @Override
    protected void getRunnerScript(StringBuilder builder) {
        getTeeClass(builder);
        getAddTestCases(builder);
        getTestRunnerScript(builder);
    }

    abstract void getTestRunnerScript(StringBuilder builder);

//
//    @Override
//    protected void getResultsScript(StringBuilder builder) {
//        // not needed - is already done by test runner
//    }

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
        builder.append( "require 'test/unit'\n");
        builder.append("Dir[SOURCE_DIR].each { |f| require f if File.file? f }\n");
    }

}
