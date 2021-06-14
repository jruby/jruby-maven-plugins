package de.saumya.mojo.ruby.script;

import de.saumya.mojo.ruby.NoopLogger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static de.saumya.mojo.ruby.script.TestDataFactories.gemScriptFactory;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class AbstractLauncherTest {

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() throws ScriptException, IOException {
        final NoopLogger logger = new NoopLogger();
        final GemScriptFactory gemScriptFactory = gemScriptFactory();

        return Arrays.asList(new Object[][]{
                {new AntLauncher(logger, gemScriptFactory)},
                {new EmbeddedLauncher(logger, gemScriptFactory)}
        });
    }

    public final AbstractLauncher launcher;

    public AbstractLauncherTest(AbstractLauncher launcher) {
        this.launcher = launcher;
    }

    @Test
    public void should_execute_command_with_args_and_return_in_output_stream() throws IOException, ScriptException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        launcher.execute(Arrays.asList("-v"), outputStream);

        final String output = outputStream.toString();
        assertThat(output).startsWith("jruby 9.2.18.0 (2.5.8)");
    }

    @Test
    public void should_execute_command_with_args_and_return_in_file() throws IOException, ScriptException {

        File outputFile = org.assertj.core.util.Files.newTemporaryFile();
        launcher.execute(Arrays.asList("-v", "--help"), outputFile);

        byte[] fileBytes = Files.readAllBytes(outputFile.toPath());
        final String output = new String(fileBytes, StandardCharsets.UTF_8);
        String[] lines = output.split("\n");
        assertThat(lines).hasSizeGreaterThan(3);
        // -v line
        assertThat(lines[0]).startsWith("jruby 9.2.18.0 (2.5.8)");
        // --help first
        assertThat(lines[1]).isEqualTo("Usage: jruby [switches] [--] [programfile] [arguments]");
    }

    @Test
    public void should_execute_command_without_args_and_fail_silently() throws IOException, ScriptException {

        // NOTE: process hangs because starts online interpreter
        if (launcher instanceof EmbeddedLauncher)
            return;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        launcher.execute(Collections.<String>emptyList());
        launcher.execute(Collections.<String>emptyList(), outputStream);

        final String output = outputStream.toString();
        assertThat(output).isEmpty();
    }

}
