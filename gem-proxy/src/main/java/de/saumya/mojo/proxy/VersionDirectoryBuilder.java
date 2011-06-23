/**
 * 
 */
package de.saumya.mojo.proxy;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

public class VersionDirectoryBuilder extends RubygemsHtmlVisitor {
    
    public static void main(String... args) throws Exception{
        String first = null;
        for(int i = 1; i < 3; i ++){
            long start = System.currentTimeMillis();
            HtmlDirectoryBuilder builder = new HtmlDirectoryBuilder();
            VersionDirectoryBuilder visitor = new VersionDirectoryBuilder("rails", 
                                                                          true, 
                                                                          builder, 
                                                                          Controller.BROKEN_GEMS.get("rails"));
            visitor.build();
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

    private final HtmlDirectoryBuilder builder;
    
    public VersionDirectoryBuilder(String gemname, boolean prereleases, HtmlDirectoryBuilder html, Set<String> brokenVersions) {
        super(gemname, prereleases, brokenVersions);
        this.builder = html;
    }

    public void build() throws IOException{
        accept(new URL("http://rubygems.org/gems/" + this.gemname + "/versions"));
    }

    protected void addVersion(String version) {
        builder.buildDirectoryLink(version);
    }
    
}