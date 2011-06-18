/**
 * 
 */
package de.saumya.mojo.proxy;

import java.io.IOException;
import java.net.URL;

public class VersionDirectoryBuilder extends RubygemsHtmlVisitor {
    
    private final HtmlDirectoryBuilder builder;
    
    public VersionDirectoryBuilder(boolean prereleases, HtmlDirectoryBuilder html) {
        super(prereleases);
        this.builder = html;
    }

    public static void main(String... args) throws Exception{
        String first = null;
        for(int i = 1; i < 3; i ++){
            long start = System.currentTimeMillis();
            HtmlDirectoryBuilder builder = new HtmlDirectoryBuilder();
            VersionDirectoryBuilder visitor = new VersionDirectoryBuilder(true, builder);
            visitor.build("rails");
            System.err.println(System.currentTimeMillis() - start);
            if(first == null){
                first = builder.toHTML();
            }
            else {
                String xml = builder.toHTML();
                System.err.println(first.equals(xml));
            }
        }
        System.err.println(first);
    }
    
    public void build(String gemname) throws IOException{
        accept(new URL("http://rubygems.org/gems/" + gemname + "/versions"));
    }

    protected void addVersion(String version) {
        builder.buildDirectoryLink(version);
    }
    
}