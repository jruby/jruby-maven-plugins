/**
 * 
 */
package de.saumya.mojo.proxy;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

public class MavenMetadataBuilder extends RubygemsHtmlVisitor {
    
    public static void main(String... args) throws Exception{
        String first = null;
        for(int i = 1; i < 5; i ++){
            long start = System.currentTimeMillis();
            MavenMetadataBuilder visitor = new MavenMetadataBuilder("rails", true, Controller.BROKEN_GEMS.get("rails"));
            visitor.build();
            System.err.println(System.currentTimeMillis() - start);
            System.out.println(visitor.toXML());
            if(first == null){
                first = visitor.toXML().replaceFirst(".*<last.*\n", "");
            }
            else {
                String xml = visitor.toXML().replaceFirst(".*<last.*\n", "");
                System.err.println(first.equals(xml));
            }
        }
    }

    public MavenMetadataBuilder(String gemname, boolean prereleases, Set<String> brokenVersions) {
        super(gemname, prereleases, brokenVersions);
    }

    private StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    
    public String toXML(){
        return xml.toString();
    }

    public void build() throws IOException{
        xml.append("<metadata>\n");
        xml.append("  <groupId>rubygems</groupId>\n");
        xml.append("  <artifactId>").append(this.gemname).append("</artifactId>\n");
        xml.append("  <versioning>\n");
        xml.append("    <versions>\n");
        accept(new URL("https://rubygems.org/gems/" + this.gemname + "/versions"));
        xml.append("    </versions>\n");
        xml.append("    <lastUpdated>")
             // hardcoded timestamp so the dynamic sha1 is correct
            .append("19990909090909")
            .append("</lastUpdated>\n");
        xml.append("  </versioning>\n");
        xml.append("</metadata>\n");
    }

    protected void addVersion(String version) {
        xml.append("      <version>").append(version).append("</version>\n");
    }
    
}
