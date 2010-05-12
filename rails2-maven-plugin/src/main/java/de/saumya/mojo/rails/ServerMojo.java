package de.saumya.mojo.rails;

/**
 * Goal to run rails with build-in server.
 * 
 * @goal server
 * @requiresDependencyResolution compile
 */
public class ServerMojo extends AbstractRailsMojo {

    public ServerMojo() {
        super("script/server");
    }

    @Override
    void addEnvironment(final StringBuilder scriptName) {
        scriptName.append(" -e ").append(this.environment);
    }

}