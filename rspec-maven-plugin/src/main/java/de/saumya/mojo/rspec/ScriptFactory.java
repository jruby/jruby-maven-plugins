package de.saumya.mojo.rspec;

import java.io.File;
import java.util.List;
import java.util.Properties;

public interface ScriptFactory {
	
	void setBaseDir(String baseDir);
	void setSummaryReport(File summaryReport);
	void setOutputDir(File outputDir);
	void setSourceDir(String sourceDir);
	void setReportPath(String reportPath);
	void setClasspathElements(List<String> classpathElements);
	void setSystemProperties(Properties systemProperties);
	void setGemHome(File gemHome);
	void setGemPaths(File[] gemPaths);
	
	File getScriptFile();
	
	String getScript() throws Exception;
	void emit() throws Exception;

}
