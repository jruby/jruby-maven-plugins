package de.saumya.mojo.rails;

/**
 * Goal to run rails console.
 * 
 * @goal console
 * 
 */
public class ConsoleMojo extends AbstractRailsMojo {

    // override super mojo and make this readonly
    /**
     * @parameter expression="false"
     * @readonly
     */
    protected boolean fork;

    public ConsoleMojo() {
        super("script/console");
    }

}