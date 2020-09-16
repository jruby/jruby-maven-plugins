package de.saumya.mojo.ruby.script;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class JRubyVersionComparisonTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"9.2.10.1", new Integer[]{9}, FALSE, "compared major is equal"},
                {"9.2.10.1", new Integer[]{9, 2}, FALSE, "compared  major is equal and minor is lower"},
                {"9.2.10.1", new Integer[]{9, 3}, TRUE, "compared  major is equal and minor is higher"},

                {"9.2.10.1", new Integer[]{10}, TRUE, "compared  major is greater and minor is equal"},
                {"9.2.10.1", new Integer[]{11, 2}, TRUE, "compared  major is equal and minor is lower"},
                {"9.2.10.1", new Integer[]{12, 3, 14}, TRUE, "compared  major is equal and minor is higher"},

                {"9.2.10.1", new Integer[]{7}, FALSE, "compared major is lower and minor is equal"},
                {"9.2.10.1", new Integer[]{6, 2}, FALSE, "compared  major is lower and minor is lower"},
                {"9.2.10.1", new Integer[]{5, 3, 14}, FALSE, "compared  major is lower and minor is higher"},

                {"9.1", new Integer[]{9, 1, 10}, FALSE, "version has more components"}
        });
    }

    private final String jrubyVersion;
    private final Integer[] versionToCompare;
    private final Boolean expectedValue;

    public JRubyVersionComparisonTest(String jrubyVersion, Integer[] versionToCompare, Boolean expectedValue, String description) {
        this.jrubyVersion = jrubyVersion;
        this.versionToCompare = versionToCompare;
        this.expectedValue = expectedValue;
    }


    @Test
    public void should_compared_jruby_version() {
        final JRubyVersion version = new JRubyVersion(jrubyVersion, "3.5.7");

        boolean result = version.isVersionLowerThan(versionToCompare);

        assertThat(result).isEqualTo(expectedValue);
    }

}
