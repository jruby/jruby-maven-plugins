package de.saumya.mojo.runit;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public abstract class AbstractMavenTestScriptFactory extends AbstractTestScriptFactory {

    public String getFullScript() throws MalformedURLException {
        StringBuilder builder = new StringBuilder();

        getInterpreterScript(builder);
        getPrologScript(builder);
        getRubygemsSetupScript(builder);

        getSystemPropertiesScript(builder);
        builder.append(getPluginClasspathScript());
        getTestClasspathSetupScript(builder);

        getConstantsConfigScript(builder);

        getRunnerScript(builder);

        getResultsScript(builder);

        return builder.toString();
    }

    private void getSystemPropertiesScript(StringBuilder builder) {
        if (systemProperties.keySet().isEmpty()) {
            return;
        }

        builder.append("# Set up system-properties for running outside of maven\n");
        builder.append("\n");

        for (Object propName : systemProperties.keySet()) {
            String propValue = systemProperties.getProperty(propName.toString());
            builder.append("Java::java.lang::System.setProperty( %q(" + propName.toString() + "), %q(" + propValue + ") )\n");
        }

        builder.append("\n");
    }

    private void getConstantsConfigScript(StringBuilder builder) {
        builder.append("# Constants used for configuration and execution\n");
        builder.append("\n");

        builder.append("BASE_DIR=%q(" + sanitize(baseDir.getAbsolutePath()) + ")\n");
        builder.append("SOURCE_DIR=%q(" + sanitize(sourceDir.getAbsolutePath()) + ")\n");
        builder.append("TARGET_DIR=%q(" + sanitize(outputDir.getAbsolutePath()) + ")\n");
        builder.append("REPORT_PATH=%q(" + sanitize(reportPath.getAbsolutePath()) + ")\n");
        if (summaryReport != null) {
            builder.append("SUMMARY_REPORT=%q(" + sanitize(summaryReport.getAbsolutePath()) + ")\n");
        }
        else {
            builder.append("SUMMARY_REPORT=nil\n");
        }
        builder.append("\n");
        builder.append("$: << File.join( BASE_DIR, 'lib' )\n");
        builder.append("$: << SOURCE_DIR\n");
        builder.append("\n");
    }

    protected abstract void getRunnerScript(StringBuilder builder);

    public String getCoreScript() {
        StringBuilder builder = new StringBuilder();

        getConstantsConfigScript(builder);
        getRunnerScript(builder);

        return builder.toString();
    }

    protected void getResultsScript(StringBuilder builder) {
        builder.append("# A little exit code magic\n");
        builder.append("\n");

        builder.append("if File.new(REPORT_PATH, 'r').read =~ /, 0 failures/ \n");
        builder.append("  exit 0\n" );
        builder.append("else\n");
        builder.append("  exit 1\n" );
        builder.append("end\n");
        builder.append("\n");
    }

    private void getInterpreterScript(StringBuilder builder) {
        builder.append("#!/usr/bin/env jruby\n");
        builder.append("\n");
    }

    private void getPrologScript(StringBuilder builder) {
        builder.append("require %(java)\n");
        builder.append("\n");
    }

    private void getRubygemsSetupScript(StringBuilder builder) {
        if (gemHome == null && gemPaths == null) {
            return;
        }

        builder.append("# Set up GEM_HOME and GEM_PATH for running outside of maven\n");
        builder.append("\n");

        if (gemHome != null) {
            builder.append("ENV['GEM_HOME']=%q(" + gemHome + ")\n");
        }

        if (gemPaths != null) {
            builder.append("ENV['GEM_PATH']=%q(");
            String separator = "";
            for(File path: gemPaths) {
                builder.append(separator + path);
                if (separator.length() == 0){
                    separator = System.getProperty("path.separator");
                }
            }
            builder.append(")\n");
        }

        builder.append("\n");
    }

    private void getTestClasspathSetupScript(StringBuilder builder) {
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
            builder.append("add_classpath_element(%Q( file://" + sanitize(path) + " ))\n");
        }

        builder.append("\n");
    }

    private String sanitize(String path) {
     String sanitized = path.replaceAll( "\\\\", "/" );

     if ( sanitized.matches( "^[a-z]:.*" ) ) {
      sanitized = sanitized.substring(0,1).toUpperCase() + sanitized.substring(1);
     }
     return sanitized;
    }

    private String getPluginClasspathScript() {

        String pathToClass = getClass().getName().replaceAll("\\.", "/") + ".class";
        URL here = getClass().getClassLoader().getResource(pathToClass);

        String herePath = here.getPath();

        if (herePath.startsWith("file:")) {
            herePath = herePath.substring(5);
            int bangLoc = herePath.indexOf("!");

            if (bangLoc > 0) {
                herePath = herePath.substring(0, bangLoc);
            }
        }

        if (herePath.endsWith(".jar")) {
            return "require %q(" + herePath + ")\n";
        } else {
            return "$: << %q(" + herePath + ")\n";
        }
    }
}
