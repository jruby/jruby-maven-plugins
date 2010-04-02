package de.saumya.mojo.rails;

/**
 * Goal to run rails generator script.
 * 
 * @goal generate
 */
public class GenerateMojo extends AbstractRailsMojo {

    public GenerateMojo() {
        super("script/generate");
    }

}