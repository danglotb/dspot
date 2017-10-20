package fr.inria.diversify.automaticbuilder;

import fr.inria.diversify.mutant.descartes.DescartesChecker;
import fr.inria.diversify.mutant.descartes.DescartesInjector;
import fr.inria.diversify.mutant.pit.GradlePitTaskAndOptions;
import fr.inria.diversify.mutant.pit.MavenPitCommandAndOptions;
import fr.inria.diversify.mutant.pit.PitResult;
import fr.inria.diversify.mutant.pit.PitResultParser;
import fr.inria.diversify.runner.InputConfiguration;
import fr.inria.diversify.util.FileUtils;
import fr.inria.diversify.util.Log;
import fr.inria.diversify.utils.DSpotUtils;
import fr.inria.stamp.Main;
import fr.inria.stamp.test.listener.TestListener;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static fr.inria.diversify.mutant.pit.MavenPitCommandAndOptions.*;
import static fr.inria.diversify.utils.AmplificationHelper.PATH_SEPARATOR;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 09/07/17.
 */
public class MavenAutomaticBuilder implements AutomaticBuilder {

    private InputConfiguration configuration;

    private String backUpPom;

    private String mavenHome;

    private static final String FILE_SEPARATOR = "/";

    private static final String POM_FILE = "pom.xml";

    MavenAutomaticBuilder(@Deprecated InputConfiguration configuration) {
        this.mavenHome = DSpotUtils.buildMavenHome(configuration);
        this.configuration = configuration;
        final String pathToPomFile = configuration.getInputProgram().getProgramDir() + FILE_SEPARATOR + POM_FILE;
        try (final BufferedReader bufferedReader = new BufferedReader(new FileReader(pathToPomFile))) {
            this.backUpPom = bufferedReader.lines().collect(Collectors.joining(System.getProperty("line.separator")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (MavenPitCommandAndOptions.descartesMode &&
                DescartesChecker.shouldInjectDescartes(pathToPomFile)) {
            DescartesInjector.injectDescartesIntoPom(pathToPomFile);
        }
    }

    @Override
    public void compile(String pathToRootOfProject) {
        this.runGoals(pathToRootOfProject, "clean", "test", "-DskipTests");
    }

    @Override
    public String buildClasspath(String pathToRootOfProject) {
        try {
            final File classpathFile = new File(pathToRootOfProject + "/target/dspot/classpath");
            if (!classpathFile.exists()) {
                this.runGoals(pathToRootOfProject, "dependency:build-classpath", "-Dmdep.outputFile=" + "target/dspot/classpath");
            }
            try (BufferedReader buffer = new BufferedReader(new FileReader(classpathFile))) {
                return buffer.lines().collect(Collectors.joining());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reset() {
        final String pathToPomFile = configuration.getInputProgram().getProgramDir() + FILE_SEPARATOR + POM_FILE;
        try {
            final FileWriter writer = new FileWriter(pathToPomFile, false);
            writer.write(this.backUpPom);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void runPit(String pathToRootOfProject, CtType<?> testClass) {
        try {
            org.apache.commons.io.FileUtils.deleteDirectory(new File(pathToRootOfProject + "/target/pit-reports"));
        } catch (Exception ignored) {

        }
        try {
            String[] phases = new String[]{PRE_GOAL_PIT, //
                    CMD_PIT_MUTATION_COVERAGE, //
                    OPT_WITH_HISTORY, //
                    OPT_TARGET_CLASSES + configuration.getProperty("filter"), //
                    OPT_VALUE_REPORT_DIR, //
                    OPT_VALUE_FORMAT, //
                    OPT_VALUE_TIMEOUT, //
                    OPT_VALUE_MEMORY, //
                    OPT_TARGET_TESTS + ctTypeToFullQualifiedName(testClass), //
                    OPT_ADDITIONAL_CP_ELEMENTS + "target/dspot/dependencies/" +
                            (configuration.getProperty(PROPERTY_ADDITIONAL_CP_ELEMENTS) != null ?
                                    "," + configuration.getProperty(PROPERTY_ADDITIONAL_CP_ELEMENTS) : ""), //
                    descartesMode ? OPT_MUTATION_ENGINE_DESCARTES : OPT_MUTATION_ENGINE_DEFAULT,
                    OPT_MUTATORS + (evosuiteMode ?
                            Arrays.stream(VALUE_MUTATORS_EVOSUITE).collect(Collectors.joining(","))
                            : VALUE_MUTATORS_ALL), //
                    configuration.getProperty(PROPERTY_EXCLUDED_CLASSES) != null ?
                            OPT_EXCLUDED_CLASSES + configuration.getProperty(PROPERTY_EXCLUDED_CLASSES) :
                            ""//
            };
            if (this.runGoals(pathToRootOfProject, phases) != 0) {
                throw new RuntimeException("Maven build failed! Enable verbose mode for more information (--verbose)");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Will convert a CtType into a list of test classes full qualified names
     * in case of abstract test classes, otherwise returns only the full qualified name
     **/
    private String ctTypeToFullQualifiedName(CtType<?> testClass) {
        if (testClass.getModifiers().contains(ModifierKind.ABSTRACT)) {
            CtTypeReference<?> referenceOfSuperClass = testClass.getReference();
            return testClass.getFactory().Class().getAll()
                    .stream()
                    .filter(ctType -> referenceOfSuperClass.equals(ctType.getSuperclass()))
                    .map(CtType::getQualifiedName)
                    .collect(Collectors.joining(","));
        } else {
            return testClass.getQualifiedName();
        }
    }

    @Override
    public void runPit(String pathToRootOfProject) {
        try {
            org.apache.commons.io.FileUtils.deleteDirectory(new File(pathToRootOfProject + "/target/pit-reports"));
        } catch (Exception ignored) {

        }
        try {
            String[] phases = new String[]{PRE_GOAL_PIT, //
                    CMD_PIT_MUTATION_COVERAGE, //
                    OPT_WITH_HISTORY, //
                    OPT_TARGET_CLASSES + configuration.getProperty("filter"), //
                    OPT_VALUE_REPORT_DIR, //
                    OPT_VALUE_FORMAT, //
                    OPT_VALUE_TIMEOUT, //
                    OPT_VALUE_MEMORY, //
                    descartesMode ? OPT_MUTATION_ENGINE_DESCARTES : OPT_MUTATION_ENGINE_DEFAULT,
                    OPT_MUTATORS + (evosuiteMode ?
                            Arrays.stream(VALUE_MUTATORS_EVOSUITE).collect(Collectors.joining(","))
                            : VALUE_MUTATORS_ALL), //
                    OPT_ADDITIONAL_CP_ELEMENTS + "target/dspot/dependencies/" +
                            (configuration.getProperty(PROPERTY_ADDITIONAL_CP_ELEMENTS) != null ?
                                    "," + configuration.getProperty(PROPERTY_ADDITIONAL_CP_ELEMENTS) : ""), //
                    configuration.getProperty(PROPERTY_EXCLUDED_CLASSES) != null ?
                            OPT_EXCLUDED_CLASSES + configuration.getProperty(PROPERTY_EXCLUDED_CLASSES) :
                            ""//
            };
            if (this.runGoals(pathToRootOfProject, phases) != 0) {
                throw new RuntimeException("Maven build failed! Enable verbose mode for more information (--verbose)");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TestListener runTest(String pathToRootOfProject, String fullQualifiedName, String... testMethodNames) {
        try {
            org.apache.commons.io.FileUtils.deleteDirectory(new File(pathToRootOfProject + "/target/surefire-reports"));
        } catch (Exception ignored) {

        }
        try {
            if (this.runGoals(pathToRootOfProject,
                    "-DadditionalClasspathElements=classes.jar",
                    "-Dtest=" + fullQualifiedName + "#" + Arrays.stream(testMethodNames).collect(Collectors.joining("+")),
                    "test") != 0) {
                throw new RuntimeException("Maven build failed! Enable verbose mode for more information (--verbose)");
            }

            return parseSurefireReport(pathToRootOfProject, fullQualifiedName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TestListener parseSurefireReport(String pathToRootOfProject, String fullQualifiedName) throws SAXException, IOException, ParserConfigurationException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        final TestListener listener = new TestListener();
        final File surefireReports = new File(pathToRootOfProject + "target/surefire-reports/TEST-" + fullQualifiedName + ".xml");
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(surefireReports);
        doc.getDocumentElement().normalize();

        for (int i = 0; i < doc.getElementsByTagName("testcase").getLength(); i++) {
            final Node testcase = doc.getElementsByTagName("testcase").item(i);
            final Description testDescription = Description.createTestDescription(
                    testcase.getAttributes().getNamedItem("classname").getNodeValue(),
                    testcase.getAttributes().getNamedItem("name").getNodeValue()
            );
            listener.getRunningTests().add(testDescription);
            if (testcase.getFirstChild() != null) {
                String exceptionAsString = testcase.getFirstChild()
                        .getNextSibling()
                        .getAttributes()
                        .getNamedItem("type")
                        .getNodeValue();
                exceptionAsString = exceptionAsString.substring(0, exceptionAsString.length() - 1);
                listener.getFailingTests().add(new Failure(testDescription, (Throwable) ClassLoader
                        .getSystemClassLoader()
                        .loadClass(exceptionAsString)
                        .newInstance()
                ));
            }
        }
        return listener;
    }

    private int runGoals(String pathToRootOfProject, String... goals) {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(Arrays.asList(goals));
        request.setPomFile(new File(pathToRootOfProject + FILE_SEPARATOR + POM_FILE));
        request.setJavaHome(new File(System.getProperty("java.home")));

		Properties properties = new Properties();
		properties.setProperty("enforcer.skip", "true");
		properties.setProperty("checkstyle.skip", "true");
		properties.setProperty("cobertura.skip", "true");
		properties.setProperty("skipITs", "true");
		properties.setProperty("rat.skip", "true");
		properties.setProperty("license.skip", "true");
		properties.setProperty("findbugs.skip", "true");
		properties.setProperty("gpg.skip", "true");
		request.setProperties(properties);

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File(this.mavenHome));
        Log.info("run maven {}", Arrays.stream(goals).collect(Collectors.joining(" ")));
        if (Main.verbose) {
            invoker.setOutputHandler(System.out::println);
            invoker.setErrorHandler(System.err::println);
        } else {
            invoker.setOutputHandler(null);
            invoker.setErrorHandler(null);
        }
        try {
            return invoker.execute(request).getExitCode();
        } catch (MavenInvocationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getOutputDirectoryPit() {
        return MavenPitCommandAndOptions.OUTPUT_DIRECTORY_PIT;
    }
}
