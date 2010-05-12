package de.saumya.mojo.rails;

/**
 * Goal to run rails generator script.
 * 
 * @goal generate
 * @requiresDependencyResolution compile
 */
public class GenerateMojo extends AbstractRailsMojo {

    public GenerateMojo() {
        super("script/generate");
    }
}