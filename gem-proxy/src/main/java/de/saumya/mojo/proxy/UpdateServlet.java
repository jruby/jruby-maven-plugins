package de.saumya.mojo.proxy;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UpdateServlet extends HttpServlet {

    private static final long serialVersionUID = -7905391668655968067L;

    private ControllerService controller;

    @Override
    public void init() throws ServletException {
        super.init();
        this.controller = (ControllerService) getServletContext().getAttribute(ControllerService.class.getName());
    }

    @Override
    protected void doGet(final HttpServletRequest req,
            final HttpServletResponse resp) throws ServletException,
            IOException {
        if (this.controller.update()) {
            log("forced update of metadata");
        }
        else {
            log("skip forced update of metadata");
        }
    }
}
