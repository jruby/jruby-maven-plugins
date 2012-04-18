package de.saumya.mojo.tests;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public interface TestScriptFactory {

 void setBaseDir(File baseDir);
 void setSummaryReport(File summaryReport);
 void setOutputDir(File outputDir);
 void setSourceDir(File sourceDir);
 void setReportPath(File reportPath);
 void setClasspathElements(List<String> classpathElements);
 void setSystemProperties(Properties systemProperties);
 void setGemHome(File gemHome);
 void setGemPaths(File[] gemPaths);

 File getScriptFile();

 String getCoreScript();
 String getFullScript() throws IOException;
 void emit() throws IOException;

}
