package de.saumya.mojo.proxy;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ControllerServlet extends HttpServlet {

    private static final long serialVersionUID = -1377408089637782007L;

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
        final String[] parts = req.getPathInfo().substring(1).split("/");
        try {
            switch (parts.length) {
            case 2: // #{name}/maven-metadata.xml
                if (parts[1].equals("maven-metadata.xml")) {
                    resp.setContentType("application/xml");
                    resp.setCharacterEncoding("UTF-8");
                    this.controller.writeMetaData(parts[0], resp.getWriter());
                }
                else if (parts[1].equals("maven-metadata.xml.sha1")) {
                    resp.setContentType("text/plain");
                    resp.setCharacterEncoding("ASCII");
                    this.controller.writeMetaDataSHA1(parts[0],
                                                      resp.getWriter());
                }
                else {
                    notFound(resp);
                }
                break;
            case 3:// #{name}/#{version}/#{name}-#{version}.{gem|pom}
                if (parts[2].endsWith(".gem")) {
                    resp.sendRedirect(this.controller.getGemLocation(parts[0],
                                                                     parts[1]));
                }
                else if (parts[2].endsWith(".gem.sha1")) {
                    resp.setContentType("text/plain");
                    resp.setCharacterEncoding("ASCII");
                    this.controller.writeGemSHA1(parts[0],
                                                 parts[1],
                                                 resp.getWriter());
                }
                else if (parts[2].endsWith(".pom")) {
                    resp.setContentType("application/xml");
                    resp.setCharacterEncoding("UTF-8");
                    this.controller.writePom(parts[0],
                                             parts[1],
                                             resp.getWriter());
                }
                else if (parts[2].endsWith(".pom.sha1")) {
                    resp.setContentType("text/plain");
                    resp.setCharacterEncoding("ASCII");
                    this.controller.writePomSHA1(parts[0],
                                                 parts[1],
                                                 resp.getWriter());
                }
                else {
                    notFound(resp);
                }
                break;
            default:
                notFound(resp);
            }
        }
        catch (final FileNotFoundException e) {
            notFound(resp);
        }
    }

    private void notFound(final HttpServletResponse resp) throws IOException {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doHead(final HttpServletRequest req,
            final HttpServletResponse resp) throws ServletException,
            IOException {
        // TODO Auto-generated method stub
        super.doHead(req, resp);
    }

    @Override
    protected long getLastModified(final HttpServletRequest req) {
        // TODO Auto-generated method stub
        return super.getLastModified(req);
    }

}
