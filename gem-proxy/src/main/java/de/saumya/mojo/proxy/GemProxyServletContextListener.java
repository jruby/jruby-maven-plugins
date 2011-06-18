package de.saumya.mojo.proxy;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class GemProxyServletContextListener implements ServletContextListener {

    public void contextDestroyed(final ServletContextEvent sce) {
    }

    public void contextInitialized(final ServletContextEvent sce) {
        try {
            sce.getServletContext().setAttribute(Controller.class.getName(),
                                                 new Controller(new File(getStorage(sce))));
            sce.getServletContext().log("registered "
                                        + Controller.class.getName());
        }
        catch (final IOException e) {
            throw new RuntimeException("error initializing controller", e);
        }
    }

    private String getStorage(final ServletContextEvent sce) {
        String value = System.getenv("GEM_PROXY_STORAGE");
        if(value == null){
            value = System.getProperty("gem.proxy.storage");
            if(value == null){
                value = sce.getServletContext().getInitParameter("gem-proxy-storage");
                if (value == null){
                    throw new RuntimeException("could not find directory location for storage:\n" +
                    		"\tsystem property       : gem.proxy.storage\n" +
                            "\tenvironment variable  : GEM_PROXY_STORAGE\n" +
                            "\tcontext init parameter: gem-proxy-storage\n");
                }
            }
        }
        return value;
    }

}
