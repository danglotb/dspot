package eu.stamp_project.dspot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.stamp_project.dspot.amplifier.Amplifier;
import eu.stamp_project.dspot.budget.Budgetizer;
import eu.stamp_project.dspot.selector.PitMutantScoreSelector;
import eu.stamp_project.dspot.selector.TestSelector;
import eu.stamp_project.utils.options.BudgetizerEnum;
import eu.stamp_project.utils.program.InputConfiguration;
import eu.stamp_project.utils.AmplificationChecker;
import eu.stamp_project.utils.AmplificationHelper;
import eu.stamp_project.utils.Counter;
import eu.stamp_project.utils.DSpotUtils;
import eu.stamp_project.utils.JUnit3Support;
import eu.stamp_project.utils.compilation.DSpotCompiler;
import eu.stamp_project.utils.json.ClassTimeJSON;
import eu.stamp_project.utils.json.ProjectTimeJSON;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * User: Simon
 * Date: 08/06/15
 * Time: 17:36
 */
public class DSpot {

    private static final Logger LOGGER = LoggerFactory.getLogger(DSpot.class);

    private List<Amplifier> amplifiers;

    private int numberOfIterations;

    private TestSelector testSelector;

    private Budgetizer budgetizer;

    private DSpotCompiler compiler;

    private ProjectTimeJSON projectTimeJSON;

    public DSpot() throws Exception {
        this(3, Collections.emptyList(), new PitMutantScoreSelector(), BudgetizerEnum.NoBudgetizer);
    }

    public DSpot(int numberOfIterations) throws Exception {
        this(numberOfIterations, Collections.emptyList(), new PitMutantScoreSelector(), BudgetizerEnum.NoBudgetizer);
    }

    public DSpot(TestSelector testSelector) throws Exception {
        this(3, Collections.emptyList(), testSelector, BudgetizerEnum.NoBudgetizer);
    }

    public DSpot(int iteration, TestSelector testSelector) throws Exception {
        this(iteration, Collections.emptyList(), testSelector, BudgetizerEnum.NoBudgetizer);
    }

    public DSpot(List<Amplifier> amplifiers) throws Exception {
        this(3, amplifiers, new PitMutantScoreSelector(), BudgetizerEnum.NoBudgetizer);
    }

    public DSpot(int numberOfIterations, List<Amplifier> amplifiers) throws Exception {
        this(numberOfIterations, amplifiers, new PitMutantScoreSelector(), BudgetizerEnum.NoBudgetizer);
    }

    public DSpot(int numberOfIterations, List<Amplifier> amplifiers, TestSelector testSelector) throws Exception {
        this(numberOfIterations, amplifiers, testSelector, BudgetizerEnum.NoBudgetizer);
    }

    public DSpot(int numberOfIterations,
                 List<Amplifier> amplifiers,
                 TestSelector testSelector,
                 BudgetizerEnum budgetizer) throws Exception {
        String dependencies = InputConfiguration.get().getDependencies();
        this.compiler = DSpotCompiler.createDSpotCompiler(InputConfiguration.get(), dependencies);
        InputConfiguration.get().setFactory(this.compiler.getLauncher().getFactory());
        this.amplifiers = new ArrayList<>(amplifiers);
        this.numberOfIterations = numberOfIterations;
        this.testSelector = testSelector;
        this.testSelector.init(InputConfiguration.get());

        String splitter = File.separator.equals("/") ? "/" : "\\\\";
        final String[] splittedPath = InputConfiguration.get().getAbsolutePathToProjectRoot().split(splitter);
        final File projectJsonFile = new File(InputConfiguration.get().getOutputDirectory() +
                File.separator + splittedPath[splittedPath.length - 1] + ".json");
        if (projectJsonFile.exists()) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            this.projectTimeJSON = gson.fromJson(new FileReader(projectJsonFile), ProjectTimeJSON.class);
        } else {
            this.projectTimeJSON = new ProjectTimeJSON(splittedPath[splittedPath.length - 1]);
        }
        this.budgetizer = budgetizer.getBugtizer(this.amplifiers);
    }

    private Stream<CtType<?>> findTestClasses(String targetTestClasses) {
        if (!targetTestClasses.contains("\\")) {
            targetTestClasses = targetTestClasses.replaceAll("\\.", "\\\\\\.").replaceAll("\\*", ".*");
        }
        Pattern pattern = Pattern.compile(targetTestClasses);
        return this.compiler.getFactory().Class().getAll().stream()
                .filter(ctType -> pattern.matcher(ctType.getQualifiedName()).matches())
                .filter(ctClass ->
                        ctClass.getMethods()
                                .stream()
                                .anyMatch(AmplificationChecker::isTest))
                .filter(InputConfiguration.isNotExcluded);
    }

    private List<CtMethod<?>> buildListOfTestMethodsToBeAmplified(CtType<?> testClass, List<String> targetTestMethods) {
        if (targetTestMethods.isEmpty()) {
            return testClass.getMethods()
                    .stream()
                    .filter(AmplificationChecker::isTest)
                    .collect(Collectors.toList());
        } else {
            return targetTestMethods.stream().flatMap(pattern ->
                    testClass.getMethods().stream()
                            .filter(ctMethod -> Pattern.compile(pattern).matcher(ctMethod.getSimpleName()).matches())
                            .collect(Collectors.toList()).stream()
            ).collect(Collectors.toList());
        }
    }

    /**
     * Amplify all the test methods of all the test classes that DSpot can find.
     * A class is considered as a test class if it contains at least one test method.
     * A method is considred as test method if it matches {@link AmplificationChecker#isTest(CtMethod)}
     *
     * @return a list of amplified test classes with amplified test methods.
     */
    public List<CtType> amplifyAllTests() {
        return this._amplifyTestClasses(InputConfiguration.get().getFactory().Class().getAll().stream()
                .filter(ctClass -> !ctClass.getModifiers().contains(ModifierKind.ABSTRACT))
                .filter(ctClass ->
                        ctClass.getMethods()
                                .stream()
                                .anyMatch(AmplificationChecker::isTest)
                ).collect(Collectors.toList()));
    }

    /**
     * Amplify the given test methods of the given test classes.
     *
     * @param testClassToBeAmplified the test class to be amplified. It can be a java regex.
     * @return a list of amplified test classes with amplified test methods.
     */
    public List<CtType> amplifyTestClass(String testClassToBeAmplified) {
        return this.amplifyTestClassesTestMethods(Collections.singletonList(testClassToBeAmplified), Collections.emptyList());
    }

    /**
     * Amplify the given test methods of the given test classes.
     *
     * @param testClassToBeAmplified the test class to be amplified. It can be a java regex.
     * @param testMethod             the test method to be amplified. It can be a java regex.
     * @return a list of amplified test classes with amplified test methods.
     */
    public List<CtType> amplifyTestClassTestMethod(String testClassToBeAmplified, String testMethod) {
        return this.amplifyTestClassesTestMethods(Collections.singletonList(testClassToBeAmplified), Collections.singletonList(testMethod));
    }

    /**
     * Amplify the given test methods of the given test classes.
     *
     * @param testClassToBeAmplified the test class to be amplified. It can be a java regex.
     * @param testMethods            the list of test methods to be amplified. This list can be a list of java regex.
     * @return a list of amplified test classes with amplified test methods.
     */
    public List<CtType> amplifyTestClassTestMethods(String testClassToBeAmplified, List<String> testMethods) {
        return this.amplifyTestClassesTestMethods(Collections.singletonList(testClassToBeAmplified), testMethods);
    }

    /**
     * Amplify the given test methods of the given test classes.
     *
     * @param testClassesToBeAmplified the list of test classes to be amplified. This list can be a list of java regex.
     * @return a list of amplified test classes with amplified test methods.
     */
    public List<CtType> amplifyTestClasses(List<String> testClassesToBeAmplified) {
        return this.amplifyTestClassesTestMethods(testClassesToBeAmplified, Collections.emptyList());
    }

    /**
     * Amplify the given test methods of the given test classes.
     *
     * @param testClassesToBeAmplified the list of test classes to be amplified. This list can be a list of java regex.
     * @return a list of amplified test classes with amplified test methods.
     */
    public List<CtType> amplifyTestClassesTestMethod(List<String> testClassesToBeAmplified, String testMethod) {
        return this.amplifyTestClassesTestMethods(testClassesToBeAmplified, Collections.singletonList(testMethod));
    }

    /**
     * Amplify the given test methods of the given test classes.
     *
     * @param testClassesToBeAmplified the list of test classes to be amplified. This list can be a list of java regex.
     * @param testMethods              the list of test methods to be amplified. This list can be a list of java regex.
     * @return a list of amplified test classes with amplified test methods.
     */
    public List<CtType> amplifyTestClassesTestMethods(List<String> testClassesToBeAmplified, List<String> testMethods) {
        final List<CtType<?>> testClassesToBeAmplifiedModel = testClassesToBeAmplified.stream()
                .flatMap(this::findTestClasses)
                .collect(Collectors.toList());
        return testClassesToBeAmplifiedModel.stream()
                .map(ctType ->
                        this._amplify(ctType, this.buildListOfTestMethodsToBeAmplified(ctType, testMethods))
                ).collect(Collectors.toList());
    }

    private List<CtType> _amplifyTestClasses(List<CtType> testClassesToBeAmplified) {
        return testClassesToBeAmplified.stream()
                .map(this::_amplifyTestClass)
                .collect(Collectors.toList());
    }

    private CtType _amplifyTestClass(CtType test) {
        return this._amplify(test, AmplificationHelper.getAllTest(test));
    }

    protected CtType _amplify(CtType test, List<CtMethod<?>> methods) {
        try {
            test = JUnit3Support.convertToJUnit4(test, InputConfiguration.get());
            Counter.reset();
            Amplification testAmplification = new Amplification(this.compiler, this.amplifiers, this.testSelector, this.budgetizer);
            final List<CtMethod<?>> filteredTestCases = this.filterTestCases(methods);
            long time = System.currentTimeMillis();
            testAmplification.amplification(test, filteredTestCases, numberOfIterations);
            final long elapsedTime = System.currentTimeMillis() - time;
            LOGGER.info("elapsedTime {}", elapsedTime);
            this.projectTimeJSON.add(new ClassTimeJSON(test.getQualifiedName(), elapsedTime));
            final CtType clone = test.clone();
            test.getPackage().addType(clone);
            final CtType<?> amplification = AmplificationHelper.createAmplifiedTest(testSelector.getAmplifiedTestCases(), clone);
            final File outputDirectory = new File(InputConfiguration.get().getOutputDirectory());
            if (!testSelector.getAmplifiedTestCases().isEmpty()) {
                LOGGER.info("Print {} with {} amplified test cases in {}", amplification.getSimpleName(),
                        testSelector.getAmplifiedTestCases().size(), InputConfiguration.get().getOutputDirectory());
                DSpotUtils.printAmplifiedTestClass(amplification, outputDirectory);
            } else {
                LOGGER.warn("DSpot could not obtain any amplified test method.");
                LOGGER.warn("You can customize the following options: --amplifiers, --test-criterion, --iteration, --budgetizer etc, and retry with a new configuration.");
            }
            testSelector.report();
            FileUtils.cleanDirectory(compiler.getSourceOutputDirectory());
            try {
                String pathToDotClass = compiler.getBinaryOutputDirectory().getAbsolutePath() + "/" +
                        test.getQualifiedName().replaceAll("\\.", "/") + ".class";
                FileUtils.forceDelete(new File(pathToDotClass));
            } catch (IOException ignored) {
                //ignored
            }
            writeTimeJson();
            InputConfiguration.get().getBuilder().reset();
            return amplification;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<CtMethod<?>> filterTestCases(List<CtMethod<?>> testMethods) {
        if (InputConfiguration.get().getExcludedTestCases().isEmpty()) {
            return testMethods;
        } else {
            final List<String> excludedTestCases = Arrays.stream(
                    InputConfiguration.get().getExcludedTestCases().split(",")
            ).collect(Collectors.toList());
            return testMethods.stream()
                    .filter(ctMethod ->
                            excludedTestCases.isEmpty() ||
                                    !excludedTestCases.contains(ctMethod.getSimpleName())
                    ).collect(Collectors.toList());
        }
    }

    private void writeTimeJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final File file = new File(InputConfiguration.get().getOutputDirectory() +
                "/" + this.projectTimeJSON.projectName + ".json");
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(gson.toJson(this.projectTimeJSON));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}