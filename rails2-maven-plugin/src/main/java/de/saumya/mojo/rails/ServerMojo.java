package de.saumya.mojo.rails;

/**
 * Goal to run rails with build-in server.
 * 
 * @goal server
 * @execute phase="initialize"
 */
public class ServerMojo extends AbstractRailsMojo {

    public ServerMojo() {
        super("script/server");
    }

}