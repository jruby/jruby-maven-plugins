package de.saumya.mojo.ruby;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;

public class PlexusHelper {

    private final DefaultPlexusContainer container;

    public PlexusHelper() throws Exception {
        this(null);
    }

    public PlexusHelper(ClassWorld classWorld) throws Exception {
        if (classWorld == null) {
            classWorld = new ClassWorld("plexus.core", Thread.currentThread()
                    .getContextClassLoader());
        }

        final ContainerConfiguration cc = new DefaultContainerConfiguration().setClassWorld(classWorld)
                .setName("ruby-tools");
        this.container = new DefaultPlexusContainer(cc);
    }

    public PlexusContainer getContainer() {
        return this.container;
    }

}
