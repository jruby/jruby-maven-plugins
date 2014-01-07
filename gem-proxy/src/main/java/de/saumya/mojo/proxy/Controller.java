package de.saumya.mojo.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.saumya.mojo.proxy.Controller.FileLocation.Type;
import de.saumya.mojo.ruby.GemScriptingContainer;

public class Controller {
    
    private static final String SHA1 = ".sha1";

    private static final String RUBYGEMS_URL = "https://rubygems.org/gems";

    private static final String RUBYGEMS_S3_URL = "http://s3.amazonaws.com/production.s3.rubygems.org/gems";
    
    public static final String[] PLATFORMS = { "-universal-java-1.5",
                                               "-universal-java-1.6",
                                               "-universal-java-1.7",
                                               "-universal-java-1.8",
                                               "-universal-java",
                                               "-universal-jruby-1.2",
                                               "-jruby",
                                               "-java",
                                               "-universal-ruby-1.8.7",
                                               "-universal-ruby-1.9.2",
                                               "-universal-ruby-1.9.3",
                                               "-universal-ruby",
                                               "" };

    static final Map<String, Set<String>> BROKEN_GEMS = new HashMap<String, Set<String>>();
    
    static {
         // activeresource-2.0.0 does not exist !!!
        Set<String> rails = new TreeSet<String>();
        rails.add("2.0.0");
        BROKEN_GEMS.put("rails", rails);

	// juby-openssl-0.7.6 can not open gem with jruby-1.6.8
        Set<String> openssl = new TreeSet<String>();
        openssl.add("0.7.6"); 
        BROKEN_GEMS.put("jruby-openssl", openssl);
    }
 
    private final File localStorage;
    
    private final GemScriptingContainer script = new GemScriptingContainer();
    
    public static class FileLocation {
        enum Type { 
            XML_CONTENT, 
            HTML_CONTENT, 
            ASCII_FILE, 
            XML_FILE, 
            REDIRECT, 
            NOT_FOUND , 
            ASCII_CONTENT, 
            REDIRECT_TO_DIRECTORY, 
            TEMP_UNAVAILABLE }
        
        public FileLocation() {
            this(null, null, null, Type.REDIRECT_TO_DIRECTORY);
        }

        public FileLocation(String message) {
            this(null, null, message, Type.NOT_FOUND);
        }

        public FileLocation(String content, Type type) {
            this(null, null, content, type);
        }
        
        public FileLocation(File local, Type type) {
            this(local, null, null, type);
        }

        public FileLocation(URL remote) {
            this(null, remote, null, Type.REDIRECT);
        }
        
        private FileLocation(File localFile, URL remoteFile, String content, Type type) {
            this.content = content;
            this.remoteUrl = remoteFile;
            this.localFile = localFile;
            this.type = type;
        }
        
        final File localFile;
        final URL remoteUrl;
        final String content;
        final Type type;
    }
     
     private final Object createPom;

     // assume there will be only one instance of this class per servlet container
     private final Set<String> fileLocks = new HashSet<String>();

     public Controller(File storage) throws IOException{
         this.localStorage = storage;
         this.localStorage.mkdirs();
         this.createPom = script.runScriptletFromClassloader("create_pom.rb");
     }
     
     public FileLocation locate(String path) throws IOException{
        // release/rubygems/name/version
        // release/rubygems/name/version/
        // release/rubygems/name/version/name-version.gem
        // release/rubygems/name/version/name-version.gem.md5
        // release/rubygems/name/version/name-version.pom
        // release/rubygems/name/version/name-version.pom.md5
        // release/rubygems/name
        // release/rubygems/name/
        // release/rubygems/name/maven-metadata.xml
        
        path = path.replaceAll("/+", "/");
        if(path.endsWith("/")){
            path += "index.html";
        }
        
        String[] parts = path.split("/");
        
        if(parts.length == 0){
            // TODO make listing with two directories 'releases', 'prereleases'
            return new FileLocation("for maven", Type.ASCII_CONTENT);
        }
        else {
            boolean prereleases = parts[0].contains("pre");
            if(parts.length > 1 && !"rubygems".equals(parts[1])){
                return notFound("Only rubygems/ groupId is supported through this proxy.");
            }
            switch(parts.length){
            case 1:
            case 2:
                // TODO make listing with one directory 'rubygems'
                return notFound("directory listing not implemented");
            case 3:
                if("index.html".equals(parts[2])) {
                    return notFound("directory listing not implemented");
                }
                else {
                    return new FileLocation();
                }
            case 4:
                if("maven-metadata.xml".equals(parts[3])){
                    return metadata(parts[2], prereleases);
                }
                else if(("maven-metadata.xml" + SHA1).equals(parts[3])){
                    return metadataSha1(parts[2], prereleases);
                }
                else if("index.html".equals(parts[3])){
                    return versionListDirectory(parts[2], path, prereleases);
                }
                else {
                    return notFound("not found");
                }
            case 5:
                String filename = parts[4].replace("-SNAPSHOT", "");
                if("index.html".equals(filename)){
                    return directory(parts[2], parts[3], path);
                }
                if(filename.endsWith(".gem")){
                    // keep it backward compatible
                    filename = filename.replace("-java.gem", ".gem");
                    File local = new File(localStorage, filename.replace(".gem", ".pom"));
                    if(!local.exists()){
                        try {
                            if (!createFiles(parts[2], parts[3])){
                                return new FileLocation(filename + " is being generated", Type.TEMP_UNAVAILABLE);
                            }
                        } catch (FileNotFoundException e) {
                            return notFound("not found");
                        }
                    }
                    String url = null;
		    for( String platform : PLATFORMS )
		    {
			url = RUBYGEMS_S3_URL + "/" + filename.replace(".gem", platform + ".gem");
			if ( exists( url ) ) {
			    break;
			}
                    }
                    return new FileLocation( new URL( url ) );
                }
                if(filename.endsWith(SHA1) || filename.endsWith(".pom")){
                    File local = new File(localStorage, filename);
                    if(!local.exists()){
                        try {
                            if (!createFiles(parts[2], parts[3])){
                                return new FileLocation(filename + " is being generated", Type.TEMP_UNAVAILABLE);
                            }
                        } catch (FileNotFoundException e) {
                            return notFound("not found");
                        }
                    }
                    return new FileLocation(local, filename.endsWith(SHA1)? Type.ASCII_FILE: Type.XML_FILE);
                }
                return notFound("not found");
            default:
                return notFound("Completely unhandleable request!");
        }
        }
    }
     
    public boolean exists(String url){
        try {
           HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
           con.setRequestMethod("HEAD");
           return con.getResponseCode() == HttpURLConnection.HTTP_OK;
        }
        catch (FileNotFoundException e) {
            //e.printStackTrace();
            return false;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private FileLocation directory(String gemname, String version, String path) throws IOException {
        HtmlDirectoryBuilder builder = new HtmlDirectoryBuilder();
        
        builder.buildHeader(path);
        
        String basename = gemname + "-" + version;
        String pomfile = basename + ".pom";
        String gemfile = basename + ".gem";
        builder.buildFileLink(pomfile);
        builder.buildFileLink(pomfile + SHA1);
        builder.buildFileLink(gemfile);
        builder.buildFileLink(gemfile + SHA1);
        
        builder.buildFooter();
        
        return new FileLocation(builder.toHTML(), Type.HTML_CONTENT);
    }
    
    private boolean createFiles(String name, String version) throws IOException {    
        String gemname = name + "-" + version.replace( "-SNAPSHOT", "" );
        try {
            synchronized (fileLocks) {
                if (fileLocks.contains(gemname)) {
                    return false;
                }
                else {
                    fileLocks.add(gemname);
                }
            }

            File gemfile = new File(this.localStorage, gemname + ".gem");
            File gemfileSha = new File(this.localStorage, gemname + ".gem" + SHA1);

            File pomfile = new File(this.localStorage, gemname + ".pom");
            File pomfileSha = new File(this.localStorage, gemname + ".pom" + SHA1);

            if (!(gemfileSha.exists() && pomfile.exists() && pomfileSha.exists())) {
		String url = null;
		for( String platform : PLATFORMS )
		{
		    url = RUBYGEMS_URL + "/" + gemname + platform + ".gem";
		    try {
			downloadGemfile(gemfile, new URL(url));
			break;
		    }
		    catch (FileNotFoundException ignore) {
		    }
		}

                String pom = createPom(gemfile);

                writeUTF8(pomfile, pom);
                writeUTF8(pomfileSha, sha1(pom));
            }
            
            // we do not keep the gemfile on disc
            gemfile.delete();
            return true;
        }
        finally {
            synchronized (fileLocks) {
                fileLocks.remove(gemname);
            }
        }
    }

    private String createPom(File gemfile) {
        // protect the script container
        synchronized (script) {
            return script.callMethod(createPom, "create", gemfile.getAbsolutePath(), String.class);            
        }
    }

    private void downloadGemfile(File gemfile, URL url) throws IOException {
        InputStream input = null;
        OutputStream output = null;
        MessageDigest sha = newSha1Digest();
        try {
            input = new BufferedInputStream(url.openStream());
            output = new BufferedOutputStream(new FileOutputStream(gemfile));
            
            int b = input.read();
            while(b != -1){
                output.write(b);
                sha.update((byte) b);
                b = input.read();
            }
        }
        finally {
            if( input != null){
                input.close();
            }
            if( output != null){
                output.close();
                writeSha(new File(gemfile.getAbsolutePath() + SHA1), sha);
            }
        }
    }

    private void writeSha(File file, MessageDigest sha) throws IOException {
        writeUTF8(file, toHex(sha.digest()));
    }

    private void writeUTF8(File file, String content) throws IOException {
        PrintWriter writer = null;
        try {
           writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), 
                                                           Charset.forName("UTF-8")));
           writer.print(content);
        }
        finally {
            if(writer != null){
                writer.close();
            }
        }
    }

    private FileLocation versionListDirectory(String name, String path, boolean prereleases) throws IOException {
        HtmlDirectoryBuilder html = new HtmlDirectoryBuilder();
        
        html.buildHeader(path);

        VersionDirectoryBuilder builder = new VersionDirectoryBuilder(name, prereleases, html, BROKEN_GEMS.get(name));
        builder.build();
        
        html.buildFileLink("maven-metadata.xml");
        html.buildFileLink("maven-metadata.xml" + SHA1);
        
        html.buildFooter();
        
        return new FileLocation(html.toHTML(), Type.HTML_CONTENT);
    }

    private FileLocation notFound(String message) {
        return new FileLocation(message);
    }

    private FileLocation metadata(String name, boolean prereleases) throws IOException {
        MavenMetadataBuilder builder = new MavenMetadataBuilder(name, prereleases, BROKEN_GEMS.get(name));
        builder.build();
        return new FileLocation(builder.toXML(), Type.XML_CONTENT);
    }

    private FileLocation metadataSha1(String name, boolean prereleases) throws IOException {
        MavenMetadataBuilder builder = new MavenMetadataBuilder(name, prereleases, BROKEN_GEMS.get(name));
        builder.build();
        return new FileLocation(sha1(builder.toXML()), Type.ASCII_CONTENT);
    }
    
    private String sha1(String text) {
        MessageDigest md = newSha1Digest();
        try {
            md.update(text.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException("should not happen", e);
        }
        return toHex(md.digest());
    }

    private MessageDigest newSha1Digest() {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("error getting sha1 instance", e); 
        }
        return md;
    }

    private String toHex(byte[] data) {
        StringBuilder buf = new StringBuilder();//data.length * 2);
        for (byte b: data) {
            if(b < 0){
                buf.append(Integer.toHexString(256 + b));
            }
            else if(b < 16) {
                buf.append('0').append(Integer.toHexString(b));
            }
            else {
                buf.append(Integer.toHexString(b));
            }
        }
        return buf.toString();
    }
}
