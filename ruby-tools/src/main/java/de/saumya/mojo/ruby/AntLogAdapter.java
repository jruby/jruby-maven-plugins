/**
 *
 */
package de.saumya.mojo.ruby;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;

class AntLogAdapter implements BuildListener {

    private final Logger logger;

    public AntLogAdapter(final Logger logger) {
        this.logger = logger;
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
            this.logger.error(event.getMessage());
            break;

        case Project.MSG_WARN:
            this.logger.warn(event.getMessage());
            break;

        case Project.MSG_INFO:
            this.logger.info(event.getMessage());
            break;

        case Project.MSG_VERBOSE:
            this.logger.debug(event.getMessage());
            break;

        case Project.MSG_DEBUG:
            this.logger.debug(event.getMessage());
            break;

        default:
            this.logger.info(event.getMessage());
            break;
        }
    }
}
