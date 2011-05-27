package de.saumya.mojo.runit;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.codehaus.plexus.util.FileUtils;

public abstract class AbstractTestScriptFactory implements TestScriptFactory {

    protected List<String> classpathElements;
    protected File summaryReport;
    protected File outputDir;
    protected File baseDir;
    protected File sourceDir;
    protected File reportPath;
    protected Properties systemProperties;
    protected File gemHome;
    protected File[] gemPaths;

    public void setClasspathElements(List<String> classpathElements) {
        this.classpathElements = classpathElements;
    }

    public void setSummaryReport(File summaryReport) {
        this.summaryReport = summaryReport;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir == null ? new File(".") : baseDir;
    }

    public void setSourceDir(File sourceDir) {
        this.sourceDir = sourceDir;
    }

    public void setReportPath(File reportPath) {
        this.reportPath = reportPath;
    }

    public void setSystemProperties(Properties systemProperties) {
        this.systemProperties = systemProperties;
    }

    public void setGemHome(File gemHome) {
        this.gemHome = gemHome;
    }

    public void setGemPaths(File[] gemPaths) {
        this.gemPaths = gemPaths;
    }

    protected abstract String getScriptName();

    public File getScriptFile() {
        return new File(new File(outputDir, "bin"), getScriptName());
    }

    public void emit() throws IOException {
        String script = getFullScript();

        File scriptFile = getScriptFile();

        scriptFile.getParentFile().mkdirs();

        FileUtils.fileWrite(scriptFile.getAbsolutePath(), "UTF-8", script);
        
        // TODO is that java-1.5 ?
        scriptFile.setExecutable(true);
    }

}
