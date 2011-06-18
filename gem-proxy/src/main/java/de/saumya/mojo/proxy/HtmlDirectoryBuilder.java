/**
 * 
 */
package de.saumya.mojo.proxy;

import java.io.IOException;

public class HtmlDirectoryBuilder  {
    
    private StringBuilder html = new StringBuilder("<!DOCTYPE html>\n");
    
    public String toHTML(){
        return html.toString();
    }

    public void buildHeader(String title) throws IOException{
        html.append("<html>\n");
        html.append("  <header>\n");
        html.append("    <title>").append(title).append("</title>\n");
        html.append("  </header>\n");
        html.append("  <body>\n");
        html.append("    <a href=\"..\">parent</a><br />\n");
        html.append("    <br />\n");
    }

    public void buildFooter() throws IOException{
        html.append("  </body>\n");
        html.append("</html>\n");
    }
    
    public void buildDirectoryLink(String dirname) {
        html.append("    <a href=\"").append(dirname).append("/\">").append(dirname).append("</a><br />\n");
    }

    public void buildFileLink(String name) {
        html.append("    <a href=\"").append(name).append("\">").append(name).append("</a><br />\n");
    }
    
}