package de.saumya.mojo.gems.spec.yaml;

import java.io.IOException;

import org.codehaus.plexus.component.annotations.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import de.saumya.mojo.gems.spec.GemSpecification;
import de.saumya.mojo.gems.spec.GemSpecificationIO;

/**
 * This is here just be able to quickly switch between snakeYaml and YamlBeans,
 * since they are both good, with their own quirks. SnakeYaml won ;) So we can
 * clear up this later.
 * 
 * @author cstamas
 */
@SuppressWarnings("deprecation")
@Component(role = GemSpecificationIO.class, hint = "yaml")
public class YamlGemSpecificationIO implements GemSpecificationIO {
    protected Yaml _yaml;

    public GemSpecification read(final String string) throws IOException {
        return readGemSpecfromYaml(string);
    }

    public String write(final GemSpecification gemspec) throws IOException {
        return writeGemSpectoYaml(gemspec);
    }

    // ==

    protected Yaml getYaml() {
        if (this._yaml == null) {
            final DumperOptions dumperOptions = new DumperOptions();
            dumperOptions.setExplicitStart(true);
            dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);

            this._yaml = new Yaml(dumperOptions);
        }

        return this._yaml;
    }

    protected GemSpecification readGemSpecfromYaml(final String gemspecString)
            throws IOException {
        // snake has some problems i could not overcome
        // return readGemSpecfromYamlWithSnakeYaml( gemspec );
        // yamlbeans makes better yaml at 1st glance
        return readGemSpecfromYamlWithSnakeYaml(gemspecString);
    }

    protected String writeGemSpectoYaml(final GemSpecification gemspec)
            throws IOException {
        // snake has some problems i could not overcome
        // return writeGemSpectoYamlWithSnakeYaml( gemspec );
        // yamlbeans makes better yaml at 1st glance
        return writeGemSpectoYamlWithSnakeYaml(gemspec);
    }

    // == SnakeYaml

    protected GemSpecification readGemSpecfromYamlWithSnakeYaml(
            final String gemspecString) throws IOException {
        return (GemSpecification) getYaml().load(gemspecString);
    }

    protected String writeGemSpectoYamlWithSnakeYaml(
            final GemSpecification gemspec) throws IOException {
        return getYaml().dump(gemspec);
    }
}
