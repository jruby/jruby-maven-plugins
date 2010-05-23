package de.saumya.mojo.proxy;

import java.io.IOException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class GemProxyServletContextListener implements ServletContextListener {

    static class Updater implements Runnable {

        private volatile boolean        isRunning = true;

        private final ControllerService controller;

        Updater(final ControllerService controller) {
            this.controller = controller;
        }

        public void run() {
            // TODO better logging via slf4j
            System.out.println("started update job");
            while (this.isRunning) {
                try {
                    Thread.sleep(60 * 1000);
                    this.controller.update();
                    System.out.println("updated metadata");
                }
                catch (final InterruptedException e) {
                }
            }
            System.out.println("stopped update job");
        }
    }

    private Updater updater;

    private Thread  thread;

    public void contextDestroyed(final ServletContextEvent sce) {
        sce.getServletContext().log("stopping background job . . .");
        this.updater.isRunning = false;
        this.thread.interrupt();
        try {
            this.thread.join();
        }
        catch (final InterruptedException e) {
        }
    }

    public void contextInitialized(final ServletContextEvent sce) {
        final JRubyService jruby = new JRubyService();
        ControllerService controller;
        sce.getServletContext().log("registering "
                + ControllerService.class.getName() + " . . .");

        try {
            controller = new ControllerService(jruby);
        }
        catch (final IOException e) {
            throw new RuntimeException("error initializing controller", e);
        }
        sce.getServletContext().log("registered "
                + ControllerService.class.getName());
        sce.getServletContext().setAttribute(ControllerService.class.getName(),
                                             controller);

        this.updater = new Updater(controller);
        this.thread = new Thread(this.updater);
        this.thread.start();
    }

}
