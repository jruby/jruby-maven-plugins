/**
 * 
 */
package de.saumya.mojo.proxy;

import java.io.IOException;
import java.net.URL;

public class MavenMetadataBuilder extends RubygemsHtmlVisitor {
    
    public MavenMetadataBuilder(boolean prereleases) {
        super(prereleases);
    }

    private StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

    public static void main(String... args) throws Exception{
        String first = null;
        for(int i = 1; i < 10; i ++){
            long start = System.currentTimeMillis();
            MavenMetadataBuilder visitor = new MavenMetadataBuilder(true);
            visitor.build("rails");
            System.err.println(System.currentTimeMillis() - start);
            if(first == null){
                first = visitor.toXML().replaceFirst(".*<last.*\n", "");
            }
            else {
                String xml = visitor.toXML().replaceFirst(".*<last.*\n", "");
                System.err.println(first.equals(xml));
            }
        }
    }
    
    public String toXML(){
        return xml.toString();
    }

    public void build(String gemname) throws IOException{
        xml.append("<metadata>\n");
        xml.append("  <groupId>rubygems</groupId>\n");
        xml.append("  <artifactId>").append(gemname).append("</artifactId>\n");
        xml.append("  <versioning>\n");
        xml.append("    <versions>\n");
        accept(new URL("http://rubygems.org/gems/" + gemname + "/versions"));
        xml.append("    </versions>\n");
        xml.append("  </versioning>\n");
        xml.append("  <lastUpdated>")
             // hardcoded timestamp so the dynamic sha1 is correct
            .append("19990909090909")
            .append("</lastUpdated>\n");
        xml.append("</metadata>\n");
    }

    protected void addVersion(String version) {
        xml.append("      <version>").append(version).append("</version>\n");
    }
    
}