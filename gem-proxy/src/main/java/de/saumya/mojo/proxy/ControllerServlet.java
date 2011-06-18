package de.saumya.mojo.proxy;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.saumya.mojo.proxy.Controller.FileLocation;

public class ControllerServlet extends HttpServlet {

    private static final long serialVersionUID = -1377408089637782007L;

    private Controller controller;

    @Override
    public void init() throws ServletException {
        super.init();
        this.controller = (Controller) getServletContext().getAttribute(Controller.class.getName());
    }

    @Override
    protected void doGet(final HttpServletRequest req,
            final HttpServletResponse resp) throws ServletException,
            IOException {
        FileLocation file = controller.locate(req.getPathInfo().substring(1));

        switch(file.type){
        case NOT_FOUND: 
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, file.content);
            break;
        case XML_CONTENT:
            resp.setContentType("application/xml");
            resp.setCharacterEncoding("utf-8");
            resp.setHeader("Vary", "Accept");
            write(resp, file.content);
            break;
        case XML_FILE:
            resp.setContentType("application/xml");
            resp.setCharacterEncoding("utf-8");
            resp.setHeader("Vary", "Accept");
            write(resp, file.localFile);
            break;
        case HTML_CONTENT:
            resp.setContentType("text/html");
            resp.setCharacterEncoding("utf-8");
            resp.setHeader("Vary", "Accept");
            write(resp, file.content);
            break;
        case ASCII_FILE:
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("ASCII");
            write(resp, file.localFile);
            break;
        case ASCII_CONTENT:
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("ASCII");
            write(resp, file.content);
            break;
        case REDIRECT_TO_DIRECTORY:
            resp.sendRedirect(req.getRequestURI() + "/");
            break;
        case REDIRECT:
            resp.sendRedirect(file.remoteUrl.toString());
            break;
        case TEMP_UNAVAILABLE:
            resp.setHeader("Retry-After", "120");//seconds
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, file.content);
            break;
        default:
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Completely unhandleable request!");
        }        
    }

    private void write(HttpServletResponse resp, File localFile) throws IOException {
        OutputStream output = resp.getOutputStream();
        InputStream input = null;
        try {
            input = new BufferedInputStream(new FileInputStream(localFile));
            int c = input.read();
            while(c != -1){
                output.write(c);
                c = input.read();
            }
        }
        finally {
            output.flush();
            if(input != null){
               input.close(); 
            }
        }
    }

    private void write(HttpServletResponse resp, String content) throws IOException {
        PrintWriter writer = resp.getWriter();
        writer.append(content);
        writer.flush();
    }
}
