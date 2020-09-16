package de.saumya.mojo.ruby.script;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JRubyLanguageComparisonTest {

    @Test
    public void should_return_false_when_compared_language_major_is_equal_and_minor_version_is_equal() {
        final JRubyVersion version = new JRubyVersion("jruby", "2.5.7");

        boolean result = version.isLanguageLowerThan(2, 5);

        assertThat(result).isFalse();
    }

    @Test
    public void should_return_false_when_compared_language_major_is_equal_and_minor_version_is_lower() {
        final JRubyVersion version = new JRubyVersion("jruby", "2.5.7");

        boolean result = version.isLanguageLowerThan(2, 4);

        assertThat(result).isFalse();
    }

    @Test
    public void should_return_true_when_compared_language_major_is_equal_and_minor_version_is_higher() {
        final JRubyVersion version = new JRubyVersion("jruby", "2.5.7");

        boolean result = version.isLanguageLowerThan(2, 6);

        assertThat(result).isTrue();
    }

    @Test
    public void should_return_true_when_compared_language_major_is_higher_and_minor_is_equal() {
        final JRubyVersion version = new JRubyVersion("jruby", "2.5.7");

        boolean result = version.isLanguageLowerThan(3, 5);

        assertThat(result).isTrue();
    }

    @Test
    public void should_return_true_when_compared_language_major_is_higher_and_minor_is_lower() {
        final JRubyVersion version = new JRubyVersion("jruby", "2.5.7");

        boolean result = version.isLanguageLowerThan(3, 3);

        assertThat(result).isTrue();
    }

    @Test
    public void should_return_true_when_compared_language_major_is_higher_and_minor_is_higher() {
        final JRubyVersion version = new JRubyVersion("jruby", "2.5.7");

        boolean result = version.isLanguageLowerThan(3, 6);

        assertThat(result).isTrue();
    }

    @Test
    public void should_return_false_when_compared_language_major_is_lower_and_minor_is_equal() {
        final JRubyVersion version = new JRubyVersion("jruby", "3.5.7");

        boolean result = version.isLanguageLowerThan(1, 5);

        assertThat(result).isFalse();
    }

    @Test
    public void should_return_false_when_compared_language_major_is_lower_and_minor_is_lower() {
        final JRubyVersion version = new JRubyVersion("jruby", "3.5.7");

        boolean result = version.isLanguageLowerThan(1, 2);

        assertThat(result).isFalse();
    }

    @Test
    public void should_return_false_when_compared_language_major_is_lower_and_minor_is_higher() {
        final JRubyVersion version = new JRubyVersion("jruby", "3.5.7");

        boolean result = version.isLanguageLowerThan(1, 10);

        assertThat(result).isFalse();
    }

    @Test
    public void should_return_false_when_compared_version_major_is_lower_and_minor_is_higher() {
        final JRubyVersion version = new JRubyVersion("9.2.10.1", "3.5.7");

        boolean result = version.isVersionLowerThan(7, 10);

        assertThat(result).isFalse();
    }
}
