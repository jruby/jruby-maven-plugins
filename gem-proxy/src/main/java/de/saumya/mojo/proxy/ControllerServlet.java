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
        // System.out.println(Arrays.toString(parts));
        try {
            switch (parts.length) {
            case 4: // {releases|prereleases}/rubygems/#{name}/maven-metadata.xml
                if (parts[3].equals("maven-metadata.xml")) {
                    resp.setContentType("application/xml");
                    resp.setCharacterEncoding("UTF-8");
                    this.controller.writeMetaData(parts[2],
                                                  resp.getWriter(),
                                                  "prereleases".equals(parts[0]));
                }
                else if (parts[3].equals("maven-metadata.xml.sha1")) {
                    resp.setContentType("text/plain");
                    resp.setCharacterEncoding("ASCII");
                    this.controller.writeMetaDataSHA1(parts[2],
                                                      resp.getWriter(),
                                                      "prereleases".equals(parts[0]));
                }
                else {
                    notFound(resp);
                }
                break;
            case 5:// {releases|prereleases}/rubygems/#{name}/#{version}/#{name}-#{version}.{gem|pom}
                if (parts[4].endsWith(".gem")) {
                    resp.sendRedirect(this.controller.getGemLocation(parts[2],
                                                                     parts[3]));
                }
                else if (parts[4].endsWith(".gem.sha1")) {
                    resp.setContentType("text/plain");
                    resp.setCharacterEncoding("ASCII");
                    this.controller.writeGemSHA1(parts[2],
                                                 parts[3],
                                                 resp.getWriter());
                }
                else if (parts[4].endsWith(".pom")) {
                    resp.setContentType("application/xml");
                    resp.setCharacterEncoding("UTF-8");
                    this.controller.writePom(parts[2],
                                             parts[3],
                                             resp.getWriter());
                }
                else if (parts[4].endsWith(".pom.sha1")) {
                    resp.setContentType("text/plain");
                    resp.setCharacterEncoding("ASCII");
                    this.controller.writePomSHA1(parts[2],
                                                 parts[3],
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
