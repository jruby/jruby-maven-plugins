package de.saumya.mojo.ruby.script;

import org.assertj.core.api.ThrowableAssert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;


public class JRubyVersionTest {

    @Test
    public void should_fail_with_exceptions_when_jruby_version_is_null() {
        final JRubyVersion version = new JRubyVersion(null, "3.5.7");

        Throwable throwable = catchThrowable(new ThrowableAssert.ThrowingCallable() {
            public void call() {
                version.isVersionLowerThan(1, 2, 3);
            }
        });

        assertThat(throwable)
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    public void should_fail_with_exceptions_when_jruby_version_is_empty() {
        final JRubyVersion version = new JRubyVersion("", "3.5.7");

        Throwable throwable = catchThrowable(new ThrowableAssert.ThrowingCallable() {
            public void call() {
                version.isVersionLowerThan(1, 2, 3);
            }
        });

        assertThat(throwable)
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    public void should_fail_with_exceptions_when_jruby_version_contains_text() {
        final JRubyVersion version = new JRubyVersion("not.a.number", "3.5.7");

        Throwable throwable = catchThrowable(new ThrowableAssert.ThrowingCallable() {
            public void call() {
                version.isVersionLowerThan(1, 2, 3);
            }
        });

        assertThat(throwable)
                .isInstanceOf(NumberFormatException.class);
    }
}
