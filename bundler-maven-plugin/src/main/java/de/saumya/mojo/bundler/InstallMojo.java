package de.saumya.mojo.bundler;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import de.saumya.mojo.gem.AbstractGemMojo;
import de.saumya.mojo.ruby.gems.GemException;
import de.saumya.mojo.ruby.script.Script;
import de.saumya.mojo.ruby.script.ScriptException;

/**
 * maven wrapper around the bundler install command.
 */
@Mojo(name = "install", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.TEST)
@Deprecated
public class InstallMojo extends AbstractGemMojo {

    /**
     * arguments for the bundler command.
     */
    @Parameter(property = "bundler.args")
    private String            bundlerArgs;

    @Parameter(property = "bundler.binstubs", defaultValue = "${project.build.directory}/bin")
    private File binStubs;

    @Parameter( property = "bundler.binstubs.shebang", defaultValue = "jruby")
    private String sheBang;

    /**
     * The classpath elements of the project being tested.
     */
    @Parameter(defaultValue = "${project.testClasspathElements", required = true, readonly = true)
    protected List<String>          classpathElements;
    
    /** 
     * Determine if --local should used.
     */
    @Parameter(property = "bundler.local", defaultValue = "true")
    protected boolean local;

    /** 
     * Determine if --quiet should used.
     */
    @Parameter( property = "bundler.quiet", defaultValue = "true")
    protected boolean quiet;

    
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
        logger.warn("bundler-maven-plugin is deprecated and is not maintained anymore");
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
                // just do the timestamping if there is target dir
                if (sha1.getParentFile().exists()) {
                    FileUtils.fileWrite(sha1, pomSha1);
                }
            }
        }
        final Script script = this.factory.newScriptFromSearchPath("bundle");
        script.addArg("install");
            if ( this.quiet ) {
                script.addArg("--quiet");
            }
            if ( this.local ) {
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
            {
                noBundlerSetupStub("bundler", "bundle", true);
            }
            {
                File setupFile = new File(binStubs, "setup");
                RubyStringBuilder builder = new RubyStringBuilder();
                this.getPrologScript(builder);
                this.getHistoryLogScript(builder);
                this.getTestClasspathSetupScript(builder);
                this.getRubygemsSetupScript(builder);
                FileUtils.fileWrite(setupFile, builder.toString());
            }
            String sep = System.getProperty("line.separator");
            String stub = IOUtil.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("stub")) + 
                sep;
            for( File f: gemsConfig.getBinDirectory().listFiles()){
                if (f.getName().equals("bundle")){
                    continue;
                }
                if (f.getName().equals("rmvn")){
                    noBundlerSetupStub("ruby-maven", "rmvn", false);
                    continue;
                }
                if (f.getName().equals("gwt")){
                    noBundlerSetupStub("ruby-maven", "gwt", false);
                    continue;
                }
                if (f.getName().equals("jetty-run")){
                    noBundlerSetupStub("ruby-maven", "jetty-run", false);
                    continue;
                }
                String[] lines = FileUtils.fileRead(f).split(sep);

                final String stubSupplement;
                if ("end".equals(lines[lines.length - 1])) {
                    // extra stuff in there, we need the last 6 lines
                    StringBuilder s = new StringBuilder();
                    for (int i = lines.length - 6; i < lines.length; ++i) {
                        s.append(lines[i].replaceFirst(", version", ""));
                        s.append("\n");
                    }
                    stubSupplement = s.toString();
                } else {
                    stubSupplement = lines[lines.length - 1].replaceFirst(", version", "");
                }

                File binstubFile = new File(binStubs, f.getName());
                if(!binstubFile.exists()){
                    if(jrubyVerbose){
                        getLog().info("create bin stub " + binstubFile);
                    }
                    FileUtils.fileWrite(binstubFile, stub + stubSupplement);
                    setExecutable(binstubFile);
                }
            }
        }
    }

    private void noBundlerSetupStub(String gem, String binFile, boolean needsClasspath) throws IOException {
        File file = new File(binStubs, binFile);
        // TODO make a stub from resource
        RubyStringBuilder script = new RubyStringBuilder();
        script.append("#!/usr/bin/env ").appendLine(sheBang);
        if (needsClasspath) {
            script.appendLine("require 'pathname'");
            script.appendLine("load(File.expand_path('../setup', Pathname.new(__FILE__).realpath))");
        }
        this.getHistoryLogScript(script);
        this.getRubygemsSetupScript(script);
        script.appendLine("require 'rubygems'"); 
        script.append("load Gem.bin_path('").append(gem).append("', '").append(binFile).appendLine("')");

        FileUtils.fileWrite(file, script.toString());
        
        setExecutable(file);
    }

    private void setExecutable(File stubFile) {
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
    
    //TODO from rspec mojo - factor out to common!
    private void getTestClasspathSetupScript(RubyStringBuilder builder) {

        builder.appendLine("if defined? JRUBY_VERSION");
        builder.appendLine("  # Set up the classpath for running outside of maven");
        builder.appendLine();

        builder.appendLine("  def add_classpath_element(element)");
        builder.appendLine("    JRuby.runtime.jruby_class_loader.addURL( Java::java.net::URL.new( element ) )");
        builder.appendLine("  end");
        builder.appendLine();

        for (String path : classpathElements) {
            if (!(path.endsWith("jar") || path.endsWith("/"))) {
                path = path + "/";
            }
            if(!path.matches("jruby-complete-")){
                builder.appendLine("  add_classpath_element(%Q( file://" + sanitize(path) + " ))");
            }
        }
        
        builder.appendLine("end");
        builder.appendLine();
    }

    //TODO from rspec mojo - factor out to common!
    private void getRubygemsSetupScript(RubyStringBuilder builder) {
        File[] gemPaths = gemsConfig.getGemPath();
        if (gemHome == null && gemPaths == null) {
            return;
        }

        builder.appendLine("# Set up GEM_HOME and GEM_PATH for running outside of maven");
        builder.appendLine();

        if (gemHome != null) {
            builder.appendLine("ENV['GEM_HOME']='" + gemHome + "'");
        }

        if (gemPaths != null) {
            builder.append("ENV['GEM_PATH']='");
            String sep = "";
            for(File path: gemPaths) {
                builder.append(sep + path);
                sep = System.getProperty("path.separator");
            }
            builder.appendLine("'");
        }

        builder.appendLine();
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
    private void getPrologScript(RubyStringBuilder builder) {

        builder.appendLine("require %(java) if defined? JRUBY_VERSION");
        builder.appendLine();
    }
    
        static class RubyStringBuilder {
            
            private final StringBuilder builder = new StringBuilder();
            
            private static final String LINE_SEPARATOR = System.getProperty("line.separator");
            
            public RubyStringBuilder append(String val){
                builder.append(val);
                return this;
            }
            public RubyStringBuilder appendLine(){
                builder.append(LINE_SEPARATOR);
                return this;
            }
            public RubyStringBuilder appendLine(String val){
                builder.append(val).append(LINE_SEPARATOR);
                return this;
            }
            
            public String toString(){
                return builder.toString();
            }
        }
    private void getHistoryLogScript(RubyStringBuilder builder) {
        builder.appendLine("log = File.join('log', 'history.log')");
        builder.appendLine("if File.exists? File.dirname(log)");
        builder.appendLine("  File.open(log, 'a') do |f|");
        builder.appendLine("    f.puts \"#{$0.sub(/.*\\//, '')} #{ARGV.join ' '}\"");
        builder.appendLine("  end");
        builder.appendLine("end");
        builder.appendLine();
    }
}
