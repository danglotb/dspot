package fr.inria.stamp;

import fr.inria.diversify.utils.DSpotUtils;
import fr.inria.diversify.utils.sosiefier.InputConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.text.DecimalFormat;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 2/14/17
 */
public class MainTest {

    private static final String PATH_SEPARATOR = System.getProperty("path.separator");

    private static final String nl = System.getProperty("line.separator");

    private static final char DECIMAL_SEPARATOR = (((DecimalFormat) DecimalFormat.getInstance()).getDecimalFormatSymbols().getDecimalSeparator());

    @Before
    public void setUp() throws Exception {
        try {
            FileUtils.deleteDirectory(new File("target/trash"));
            FileUtils.deleteDirectory(new File("src/test/resources/test-projects/target"));
        } catch (Exception ignored) {

        }
    }

    @After
    public void tearDown() throws Exception {
        try {
            FileUtils.deleteDirectory(new File("target/trash"));
            FileUtils.deleteDirectory(new File("src/test/resources/test-projects/target"));
        } catch (Exception ignored) {

        }
    }

    @Test
    public void testOnProjectWithResources() throws Exception {
        Main.main(new String[]{
                "--verbose",
                "--path-to-properties", "src/test/resources/project-with-resources/project-with-resources.properties",
                "--test-criterion", "JacocoCoverageSelector",
                "--iteration", "1"
        });
    }

    @Test
    public void testDefaultModeIsNoneAmplifier() throws Exception {
        Main.main(new String[]{
                "--verbose",
                "--path-to-properties", "src/test/resources/test-projects/test-projects.properties",
                "--test-criterion", "JacocoCoverageSelector",
                "--iteration", "1"
        });
        // We test that the amplification fail on our example TODO maybe we should add a new resource that can be amplified only using A-Amplification...
        final File reportFile = new File("target/trash/example.TestSuiteExample_jacoco_instr_coverage_report.txt");
        assertTrue(reportFile.exists());
        assertTrue(new File("target/trash/example.TestSuiteExample_jacoco_instr_coverage.json").exists());
        assertTrue(new File("target/trash/example/TestSuiteExampleAmpl.java").exists());
        try (BufferedReader reader = new BufferedReader(new FileReader(reportFile))) {
            String content = reader.lines().collect(Collectors.joining(nl));
            assertEquals(expectedReportOnDefaultMode, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testNoneAmplifier() throws Exception {
        Main.main(new String[]{
                "--verbose",
                "--path-to-properties", "src/test/resources/test-projects/test-projects.properties",
                "--test-criterion", "JacocoCoverageSelector",
                "--amplifiers", "None",
                "--iteration", "1"
        });
        // We test that the amplification fail on our example TODO maybe we should add a new resource that can be amplified only using A-Amplification...
        final File reportFile = new File("target/trash/example.TestSuiteExample_jacoco_instr_coverage_report.txt");
        assertTrue(reportFile.exists());
        assertTrue(new File("target/trash/example.TestSuiteExample_jacoco_instr_coverage.json").exists());
        assertTrue(new File("target/trash/example/TestSuiteExampleAmpl.java").exists());
        try (BufferedReader reader = new BufferedReader(new FileReader(reportFile))) {
            String content = reader.lines().collect(Collectors.joining(nl));
            assertEquals(expectedReportOnDefaultMode, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String expectedReportOnDefaultMode = nl +
            "======= REPORT =======" + nl +
            "Initial instruction coverage: 33 / 37" + nl +
            "89.19%" + nl +
            "Amplification results with 0 amplified tests." + nl +
            "Amplified instruction coverage: 33 / 37" + nl +
            "89.19%";

    @Test
    public void testExample() throws Exception {

        /*
            Test the --example option. It runs a specific predefined example of amplification.
                It also checks the auto imports output of DSpot.
         */

        Main.main(new String[]{"--verbose", "--example"});
        final File reportFile = new File("target/trash/example.TestSuiteExample_jacoco_instr_coverage_report.txt");
        assertTrue(reportFile.exists());
        assertTrue(new File("target/trash/example.TestSuiteExample_jacoco_instr_coverage.json").exists());
        assertTrue(new File("target/trash/example/TestSuiteExampleAmpl.java").exists());
        try (BufferedReader reader = new BufferedReader(new FileReader(reportFile))) {
            String content = reader.lines().collect(Collectors.joining(nl));
            assertEquals(expectedReportExample, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(new File("target/trash/example/TestSuiteExampleAmpl.java")))) {
            String content = reader.lines().collect(Collectors.joining(nl));
            assertTrue(content.startsWith(expectedAmplifiedTestClass));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //we  don't test the whole file, but only the begin of it. It is sufficient to detect the auto import.
    private static final String expectedAmplifiedTestClass = "package example;" + nl  +
            "" + nl  +
            "" + nl  +
            "import org.junit.Assert;" + nl  +
            "import org.junit.Test;" + nl  +
            "" + nl  +
            "" + nl  +
            "public class TestSuiteExampleAmpl {" + nl  +
            "    /* amplification of example.TestSuiteExample#test2 */" + nl  +
            "    @Test(timeout = 10000)" + nl  +
            "    public void test2_literalMutationString2() {" + nl  +
            "        Example ex = new Example();" + nl  +
            "        // AssertGenerator create local variable with return value of invocation" + nl  +
            "        char o_test2_literalMutationString2__3 = ex.charAt(\"acd\", 3);" + nl  +
            "        // AssertGenerator add assertion" + nl  +
            "        Assert.assertEquals('d', ((char) (o_test2_literalMutationString2__3)));" + nl  +
            "    }";

    @Test
    public void testTwoClasses() throws Exception {
        Main.main(new String[]{
                "--path-to-properties", "src/test/resources/test-projects/test-projects.properties",
                "--test-criterion", "JacocoCoverageSelector",
                "--amplifiers", "MethodAdd" + PATH_SEPARATOR + "TestDataMutator" + PATH_SEPARATOR + "StatementAdd",
                "--iteration", "1",
                "--randomSeed", "72",
                "--maven-home", DSpotUtils.buildMavenHome(new InputConfiguration("src/test/resources/test-projects/test-projects.properties")),
                "--test", "example.TestSuiteExample:example.TestSuiteExample2",
                "--output-path", "target/trash",
                "--max-test-amplified", "200"
        });
        final File reportFile = new File("target/trash/example.TestSuiteExample_jacoco_instr_coverage_report.txt");
        assertTrue(reportFile.exists());
        assertTrue(new File("target/trash/example.TestSuiteExample_jacoco_instr_coverage.json").exists());
        assertTrue(new File("target/trash/example/TestSuiteExampleAmpl.java").exists());
        try (BufferedReader reader = new BufferedReader(new FileReader(reportFile))) {
            String content = reader.lines().collect(Collectors.joining(nl));
            assertEquals(expectedReportAll, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testOneClassOneMethod() throws Throwable {
        Main.main(new String[]{
                "--path-to-properties", "src/test/resources/test-projects/test-projects.properties",
                "--test-criterion", "JacocoCoverageSelector",
                "--amplifiers", "MethodAdd" + PATH_SEPARATOR + "TestDataMutator" + PATH_SEPARATOR + "StatementAdd",
                "--iteration", "1",
                "--randomSeed", "72",
                "--maven-home", DSpotUtils.buildMavenHome(new InputConfiguration("src/test/resources/test-projects/test-projects.properties")),
                "--test", "example.TestSuiteExample",
                "--cases", "test2",
                "--output-path", "target/trash",
                "--max-test-amplified", "200"
        });
        final File reportFile = new File("target/trash/example.TestSuiteExample_jacoco_instr_coverage_report.txt");
        assertTrue(reportFile.exists());
        assertTrue(new File("target/trash/example.TestSuiteExample_jacoco_instr_coverage.json").exists());
        assertTrue(new File("target/trash/example/TestSuiteExampleAmpl.java").exists());
        try (BufferedReader reader = new BufferedReader(new FileReader(reportFile))) {
            String content = reader.lines().collect(Collectors.joining(nl));
            assertEquals(expectedReportOneClassOneMethod, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String expectedReportOneClassOneMethod = nl +
            "======= REPORT =======" + nl +
            "Initial instruction coverage: 33 / 37" + nl +
            "89" + DECIMAL_SEPARATOR + "19%" + nl +
            "Amplification results with 5 amplified tests." + nl +
            "Amplified instruction coverage: 37 / 37" + nl +
            "100" + DECIMAL_SEPARATOR + "00%";

    @Test
    public void testRegexOnWholePackage() throws Throwable {
        Main.main(new String[]{
                "--path-to-properties", "src/test/resources/test-projects/test-projects.properties",
                "--test-criterion", "JacocoCoverageSelector",
                "--amplifiers", "MethodAdd" + PATH_SEPARATOR + "TestDataMutator" + PATH_SEPARATOR + "StatementAdd",
                "--iteration", "1",
                "--randomSeed", "72",
                "--maven-home", DSpotUtils.buildMavenHome(new InputConfiguration("src/test/resources/test-projects/test-projects.properties")),
                "--test", "example.*",
                "--output-path", "target/trash",
                "--max-test-amplified", "200"
        });
        final File reportFile = new File("target/trash/example.TestSuiteExample_jacoco_instr_coverage_report.txt");
        assertTrue(reportFile.exists());
        final File reportFile2 = new File("target/trash/example.TestSuiteExample2_jacoco_instr_coverage_report.txt");
        assertTrue(reportFile2.exists());
        assertTrue(new File("target/trash/example.TestSuiteExample_jacoco_instr_coverage.json").exists());
        assertTrue(new File("target/trash/example.TestSuiteExample2_jacoco_instr_coverage.json").exists());
        assertTrue(new File("target/trash/example/TestSuiteExampleAmpl.java").exists());
        assertTrue(new File("target/trash/example/TestSuiteExample2Ampl.java").exists());
        try (BufferedReader reader = new BufferedReader(new FileReader(reportFile))) {
            String content = reader.lines().collect(Collectors.joining(nl));
            assertEquals(expectedReportAll, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testUsingRegex() throws Throwable {
        Main.main(new String[]{
                "--path-to-properties", "src/test/resources/test-projects/test-projects.properties",
                "--test-criterion", "JacocoCoverageSelector",
                "--amplifiers", "MethodAdd" + PATH_SEPARATOR + "TestDataMutator" + PATH_SEPARATOR + "StatementAdd",
                "--iteration", "1",
                "--randomSeed", "72",
                "--maven-home", DSpotUtils.buildMavenHome(new InputConfiguration("src/test/resources/test-projects/test-projects.properties")),
                "--test", "example.TestSuite*",
                "--output-path", "target/trash",
                "--max-test-amplified", "200"
        });
        final File reportFile = new File("target/trash/example.TestSuiteExample_jacoco_instr_coverage_report.txt");
        assertTrue(reportFile.exists());
        assertTrue(new File("target/trash/example.TestSuiteExample_jacoco_instr_coverage.json").exists());
        assertTrue(new File("target/trash/example/TestSuiteExampleAmpl.java").exists());
        try (BufferedReader reader = new BufferedReader(new FileReader(reportFile))) {
            String content = reader.lines().collect(Collectors.joining(nl));
            assertEquals(expectedReportAll, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testAll() throws Throwable {
        Main.main(new String[]{
                "--path-to-properties", "src/test/resources/test-projects/test-projects.properties",
                "--test-criterion", "JacocoCoverageSelector",
                "--amplifiers", "MethodAdd" + PATH_SEPARATOR + "TestDataMutator" + PATH_SEPARATOR + "StatementAdd",
                "--iteration", "1",
                "--randomSeed", "72",
                "--maven-home", DSpotUtils.buildMavenHome(new InputConfiguration("src/test/resources/test-projects/test-projects.properties")),
                "--test", "all",
                "--output-path", "target/trash",
                "--max-test-amplified", "200"
        });
        final File reportFile = new File("target/trash/example.TestSuiteExample_jacoco_instr_coverage_report.txt");
        assertTrue(reportFile.exists());
        assertTrue(new File("target/trash/example.TestSuiteExample_jacoco_instr_coverage.json").exists());
        assertTrue(new File("target/trash/example/TestSuiteExampleAmpl.java").exists());
        try (BufferedReader reader = new BufferedReader(new FileReader(reportFile))) {
            String content = reader.lines().collect(Collectors.joining(nl));
            assertEquals(expectedReportAll, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String expectedReportExample = nl +
            "======= REPORT =======" + nl +
            "Initial instruction coverage: 33 / 37" + nl +
            "89" + DECIMAL_SEPARATOR + "19%" + nl +
            "Amplification results with 27 amplified tests." + nl +
            "Amplified instruction coverage: 37 / 37" + nl +
            "100" + DECIMAL_SEPARATOR + "00%";

    private static final String expectedReportAll = nl +
            "======= REPORT =======" + nl +
            "Initial instruction coverage: 33 / 37" + nl +
            "89" + DECIMAL_SEPARATOR + "19%" + nl +
            "Amplification results with 31 amplified tests." + nl +
            "Amplified instruction coverage: 37 / 37" + nl +
            "100" + DECIMAL_SEPARATOR + "00%";

}
