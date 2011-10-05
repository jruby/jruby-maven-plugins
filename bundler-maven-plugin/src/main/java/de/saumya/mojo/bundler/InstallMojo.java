package de.saumya.mojo.bundler;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.sonatype.aether.RepositorySystemSession;

import de.saumya.mojo.gem.AbstractGemMojo;
import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * maven wrapper around the bundler install command.
 * 
 * @goal install
 * @phase initialize
 * @requiresDependencyResolution test
 */
public class InstallMojo extends AbstractGemMojo {

    /**
     * arguments for the bundler command.
     * 
     * @parameter default-value="${bundler.args}"
     */
    private String            bundlerArgs;

    /**
     * @parameter default-value="${project.build.directory}/bin" expression="${bundler.binstubs}"
     */
    private File binStubs;
    
    /**
     * bundler version used when there is no pom. defaults to latest version.
     * 
     * @parameter default-value="${bundler.version}"
     */
    private String            bundlerVersion;

    /**
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    private RepositorySystemSession repoSession;

    /**
     * The classpath elements of the project being tested.
     * 
     * @parameter expression="${project.testClasspathElements}"
     * @required
     * @readonly
     */
    protected List<String>          classpathElements;
    
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

    @Override
    public void executeWithGems() throws MojoExecutionException,
            ScriptException, IOException, GemException {
        if(project.getFile() != null){
            String pomSha1 = sha1(FileUtils.fileRead(project.getFile()));
            File sha1 = new File(project.getBuild().getDirectory(), project.getFile().getName() + ".sha1");
            if(sha1.exists()){
                String oldPomSha1 = FileUtils.fileRead(sha1);
                if (pomSha1.equals(oldPomSha1)) {
                    if(jrubyVerbose){
                        getLog().info("skip bundler install since pom did not change since last run");
                    }
                    return;
                }
                else{
                    FileUtils.fileWrite(sha1, pomSha1);
                }
            }
            else{
                FileUtils.fileWrite(sha1, pomSha1);
            }
        }
        final Script script = this.factory.newScriptFromSearchPath("bundle");
        script.addArg("install");
        if (this.project.getBasedir() == null) {

            this.gemsInstaller.installGem("bundler",
                                          this.bundlerVersion,
                                          this.repoSession,
                                          this.localRepository);

        }
        else {
            script.addArg("--quiet");
            script.addArg("--local");
        }
        if (this.bundlerArgs != null) {
            script.addArgs(this.bundlerArgs);
        }
        if (this.args != null) {
            script.addArgs(this.args);
        }

        script.executeIn(launchDirectory());
        
        generateBinStubs();
    }

    private void generateBinStubs() throws IOException {
        if(binStubs != null){
            binStubs.mkdirs();
            File stubFile = new File(binStubs, "setup");
            FileUtils.fileWrite(stubFile, this.getPrologScript() + this.getTestClasspathSetupScript() + getRubygemsSetupScript());
            String sep = System.getProperty("line.separator");
            String stub = IOUtil.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("stub")) + 
                sep;
            for( File f: gemsConfig.getBinDirectory().listFiles()){
                String[] lines = FileUtils.fileRead(f).split(sep);
                stubFile = new File(binStubs, f.getName());
                if(!stubFile.exists()){
                    if(jrubyVerbose){
                        getLog().info("create bin stub " + stubFile);
                    }
                    FileUtils.fileWrite(stubFile, stub + lines[lines.length - 1].replaceFirst(", version", ""));
                    try {
                        // use reflection so it compiles with java1.5 as well but does not set executable
                        Method m = stubFile.getClass().getDeclaredMethod("setExecutable", boolean.class);
                        m.invoke(stubFile, new Boolean(true));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        getLog().warn("can not set executable flag: "
                                + stubFile.getAbsolutePath() + " (" + e.getMessage() + ")");
                    }
                }
            }
        }
    }
    
    //TODO from rspec mojo - factor out to common!
    private String getTestClasspathSetupScript() {
        StringBuilder builder = new StringBuilder();

        builder.append("# Set up the classpath for running outside of maven\n");
        builder.append("\n");

        builder.append("def add_classpath_element(element)\n");
        builder.append("  JRuby.runtime.jruby_class_loader.addURL( Java::java.net::URL.new( element ) )\n");
        builder.append("end\n");
        builder.append("\n");

        for (String path : classpathElements) {
            if (!(path.endsWith("jar") || path.endsWith("/"))) {
                path = path + "/";
            }
            if(!path.matches("jruby-complete-")){
                builder.append("add_classpath_element(%Q( file://" + sanitize(path) + " ))\n");
            }
        }
        
        builder.append("\n");

        return builder.toString();
    }

    //TODO from rspec mojo - factor out to common!
    private String getRubygemsSetupScript() {
        File[] gemPaths = gemsConfig.getGemPath();
        if (gemHome == null && gemPaths == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        builder.append("# Set up GEM_HOME and GEM_PATH for running outside of maven\n");
        builder.append("\n");

        if (gemHome != null) {
            builder.append("ENV['GEM_HOME']='" + gemHome + "'\n");
        }

        if (gemPaths != null) {
            builder.append("ENV['GEM_PATH']='");
            String sep = "";
            for(File path: gemPaths) {
                builder.append(sep + path);
                sep = System.getProperty("path.separator");
            }
            builder.append("'\n");
        }

        builder.append("\n");

        return builder.toString();
    }

    //TODO from rspec mojo - factor out to common!
    private String sanitize(String path) {
        String sanitized = path.replaceAll( "\\\\", "/" );
        
        if ( sanitized.matches( "^[a-z]:.*" ) ) {
            sanitized = sanitized.substring(0,1).toUpperCase() + sanitized.substring(1);
        }
        return sanitized;
    }
    //TODO from rspec mojo - factor out to common!
    private String getPrologScript() {
        StringBuilder builder = new StringBuilder();

        builder.append("require %(java)\n");
        builder.append("\n");

        return builder.toString();
    }

}
