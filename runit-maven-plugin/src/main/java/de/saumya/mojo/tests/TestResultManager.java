package de.saumya.mojo.tests;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.codehaus.plexus.util.FileUtils;

import de.saumya.mojo.jruby.JRubyVersion;
import de.saumya.mojo.jruby.JRubyVersion.Mode;
import de.saumya.mojo.tests.JRubyRun.Result;

public class TestResultManager {

    enum ResultEnum {
        TESTS, ASSERTIONS, FAILURES, ERRORS, SKIPS
    }

    private final String projectName;
    private final File testReportDirectory;
    private final File summaryReport;
    private final String filename;
    
    public TestResultManager(File summaryReport){
        this(null, null, null, summaryReport);
    }

    public TestResultManager(String projectName, String filename, File testReportDirectory, File summaryReport){
        this.projectName = projectName;
        this.filename = filename == null ? null : "TEST-" + filename;
        this.testReportDirectory = testReportDirectory;
        this.summaryReport = summaryReport;
    }
    
    public Result generateReports(Mode mode, JRubyVersion version,
            final File outputfile) throws IOException {
        Result result = new Result();
        String time = null;
        for (Object lineObj : FileUtils.loadFile(outputfile)) {
            String line = lineObj.toString();
            if (line.contains("Finished")) {
                time = line.replaceFirst(",.*$", "").replaceAll("[a-zA-Z]+", "").trim();
            }
            if (line.contains("failures")) {
                result.message = line;
                int[] vector = new int[5];
                int i = 0;
                String statusLine = line.replaceAll("[a-z]+,?", "");
                for (String n : statusLine.split("\\s+")) {
                    vector[i++] = Integer.parseInt(n);
                }
                result.success = (vector[ResultEnum.FAILURES.ordinal()] == 0)
                        && (vector[ResultEnum.ERRORS.ordinal()] == 0);

                if (filename != null || summaryReport != null) {
                    // TODO should be a real report with testcases
                    String surefireXml = MessageFormat
                            .format("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                                    + "<testsuite time=\"{0}\" errors=\"{1}\" tests=\"{2}\" skipped=\"{3}\" failures=\"{4}\" name=\"{5}\">\n"
                                    + "</testsuite>\n", time,
                                    vector[ResultEnum.ERRORS.ordinal()],
                                    vector[ResultEnum.TESTS.ordinal()],
                                    vector[ResultEnum.SKIPS.ordinal()],
                                    vector[ResultEnum.FAILURES.ordinal()],
                                    this.projectName);

                    if (filename != null) {
                        testReportDirectory.mkdirs();
                        String filename = this.filename
                                + (version == null ? "" : "-" + version ) + (mode == null ? "" : mode.flag) + ".xml";
                        FileUtils.fileWrite(new File(testReportDirectory,
                                filename).getAbsolutePath(), "UTF-8",
                                surefireXml);
                    }
                    if (summaryReport != null) {
                        FileUtils.fileWrite(summaryReport.getAbsolutePath(),
                                "UTF-8", surefireXml);
                    }
                }
                return result;
            }
        }
        result.message = "did not find test summary";
        result.success = false;
        return result;
    }
}