package de.saumya.mojo.gems.spec.yaml;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import de.saumya.mojo.gems.spec.GemDependency;
import de.saumya.mojo.gems.spec.GemRequirement;
import de.saumya.mojo.gems.spec.GemSpecification;
import de.saumya.mojo.gems.spec.GemVersion;

/**
 * A helper for snakeYaml.
 * 
 * @author cstamas
 */
public class MappingRepresenter extends Representer {
    public MappingRepresenter() {
        super();

        this.nullRepresenter = new RepresentNull();

        this.addClassTag(GemSpecification.class,
                         new Tag("!ruby/object:Gem::Specification"));
        this.addClassTag(GemDependency.class,
                         new Tag("!ruby/object:Gem::Dependency"));
        this.addClassTag(GemRequirement.class,
                         new Tag("!ruby/object:Gem::Requirement"));
        this.addClassTag(GemVersion.class, new Tag("!ruby/object:Gem::Version"));
    }

    private class RepresentNull implements Represent {
        public Node representData(final Object data) {
            return representScalar(Tag.NULL, "null");
        }
    }
}
