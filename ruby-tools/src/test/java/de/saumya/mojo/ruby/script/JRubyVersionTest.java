package de.saumya.mojo.ruby.script;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JRubyVersionTest {

    @Test
    public void should_return_true_when_compared_language_version_is_lower_than_major() {
        final JRubyVersion version = new JRubyVersion("2.5.7", "2.5.7");

        boolean result = version.isLanguageLowerThan(1, 5);

        assertThat(result).isTrue();
    }

    @Test
    public void should_return_true_when_compared_language_version_is_lower_than_minor() {
        final JRubyVersion version = new JRubyVersion("2.5.7", "2.5.7");

        boolean result = version.isLanguageLowerThan(2, 4);

        assertThat(result).isTrue();
    }

    @Test
    public void should_return_true_when_compared_language_version_is_equal() {
        final JRubyVersion version = new JRubyVersion("2.5.7", "2.5.7");

        boolean result = version.isLanguageLowerThan(2, 5);

        assertThat(result).isFalse();
    }

    @Test
    public void should_return_false_when_compared_language_version_is_higher_than_major() {
        final JRubyVersion version = new JRubyVersion("2.5.7", "2.5.7");

        boolean result = version.isLanguageLowerThan(3, 5);

        assertThat(result).isFalse();
    }

    @Test
    public void should_return_false_when_compared_language_version_is_higher_than_minor() {
        final JRubyVersion version = new JRubyVersion("2.5.7", "2.5.7");

        boolean result = version.isLanguageLowerThan(2, 6);

        assertThat(result).isFalse();
    }
}
