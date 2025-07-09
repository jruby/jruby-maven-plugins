package de.saumya.mojo.ruby.script;

import org.jruby.Main;
import org.jruby.runtime.Constants;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static de.saumya.mojo.ruby.script.TestDataFactories.gemScriptFactory;
import static org.assertj.core.api.Assertions.assertThat;

public class ScriptFactoryTest {

    private static final String BASE_JRUBY_VERSION_STRING = "jruby " + Constants.VERSION + " (" + Constants.RUBY_VERSION + ")";

    @Test
    public void should_execute_script_with_only_args_and_return_in_output_stream() throws ScriptException, IOException {
        Main main = new Main();
        final GemScriptFactory gemScriptFactory = gemScriptFactory();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        gemScriptFactory.newArguments()
                .addArg("-v")
                .execute(outputStream);

        final String output = outputStream.toString();
        assertThat(output).startsWith(BASE_JRUBY_VERSION_STRING);
    }

    @Test
    public void should_execute_script_with_only_args_and_return_in_file() throws ScriptException, IOException {
        Main main = new Main();
        final GemScriptFactory gemScriptFactory = gemScriptFactory();

        File outputFile = org.assertj.core.util.Files.newTemporaryFile();
        gemScriptFactory.newArguments()
                .addArg("-v")
                .execute(outputFile);

        byte[] fileBytes = Files.readAllBytes(outputFile.toPath());
        final String output = new String(fileBytes, StandardCharsets.UTF_8);
        assertThat(output).startsWith(BASE_JRUBY_VERSION_STRING);
    }

    @Test
    public void should_return_jruby_version() throws ScriptException, IOException {
        final GemScriptFactory gemScriptFactory = gemScriptFactory();

        JRubyVersion version = gemScriptFactory.getVersion();

        assertThat(version.getVersion()).isEqualTo(Constants.VERSION);
        assertThat(version.getLanguage()).isEqualTo(Constants.RUBY_VERSION);
    }
}
