/**
 * 
 */
package de.saumya.mojo.jruby;

import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;

public class LogAdapter implements BuildListener {
    private final Log log;

    public LogAdapter(final Log log) {
        this.log = log;
    }

    public void buildStarted(final BuildEvent event) {
        log(event);
    }

    public void buildFinished(final BuildEvent event) {
        log(event);
    }

    public void targetStarted(final BuildEvent event) {
        log(event);
    }

    public void targetFinished(final BuildEvent event) {
        log(event);
    }

    public void taskStarted(final BuildEvent event) {
        log(event);
    }

    public void taskFinished(final BuildEvent event) {
        log(event);
    }

    public void messageLogged(final BuildEvent event) {
        log(event);
    }

    private void log(final BuildEvent event) {
        final int priority = event.getPriority();
        switch (priority) {
        case Project.MSG_ERR:
            this.log.error(event.getMessage());
            break;

        case Project.MSG_WARN:
            this.log.warn(event.getMessage());
            break;

        case Project.MSG_INFO:
            this.log.info(event.getMessage());
            break;

        case Project.MSG_VERBOSE:
            this.log.debug(event.getMessage());
            break;

        case Project.MSG_DEBUG:
            this.log.debug(event.getMessage());
            break;

        default:
            this.log.info(event.getMessage());
            break;
        }
    }
}