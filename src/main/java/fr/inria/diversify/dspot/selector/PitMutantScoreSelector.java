package fr.inria.diversify.dspot.selector;

import fr.inria.diversify.buildSystem.android.InvalidSdkException;
import fr.inria.diversify.dspot.DSpotUtils;
import fr.inria.diversify.dspot.support.Counter;
import fr.inria.diversify.dspot.support.DSpotCompiler;
import fr.inria.diversify.mutant.pit.PitResult;
import fr.inria.diversify.mutant.pit.PitRunner;
import fr.inria.diversify.runner.InputConfiguration;
import fr.inria.diversify.runner.InputProgram;
import fr.inria.diversify.util.FileUtils;
import fr.inria.diversify.util.InitUtils;
import fr.inria.diversify.util.Log;
import fr.inria.diversify.util.PrintClassUtils;
import org.json.JSONException;
import org.json.JSONObject;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 1/5/17
 */
public class PitMutantScoreSelector implements TestSelector {

    private InputProgram program;

    private InputConfiguration configuration;

    private CtType currentClassTestToBeAmplified;

    private List<PitResult> originalPitResults;

    private Map<CtMethod, List<PitResult>> currentMutantsKilledPerTestCase;

    private List<CtMethod> testAlreadyRun;

    private List<CtMethod> testAlreadyAdded;

    private Map<CtMethod, List<PitResult>> testThatKilledMutants;

    private int nbOfTotalMutantKilled;

    public PitMutantScoreSelector() {
        this.nbOfTotalMutantKilled = 0;
        this.testThatKilledMutants = new HashMap<>();
        this.currentMutantsKilledPerTestCase = new HashMap<>();
        this.testAlreadyRun = new ArrayList<>();
        this.testAlreadyAdded = new ArrayList<>();
        this.originalPitResults = new ArrayList<>();
    }

    //TODO That the configuration is well set up and well used.
    //TODO maybe we can do it one time for the whole project.
    @Override
    public void init(InputConfiguration configuration) {
        this.configuration = configuration;
        this.reset();
        try {
            InitUtils.initLogLevel(configuration);
            this.program = InitUtils.initInputProgram(this.configuration);
            String outputDirectory = configuration.getProperty("tmpDir") + "/tmp_pit_" + System.currentTimeMillis();
            FileUtils.copyDirectory(new File(this.program.getProgramDir()), new File(outputDirectory));
            this.program.setProgramDir(outputDirectory);
            InitUtils.initDependency(configuration);
            String mavenHome = configuration.getProperty("maven.home", null);
            String mavenLocalRepository = configuration.getProperty("maven.localRepository", null);
            DSpotUtils.compile(this.program, mavenHome, mavenLocalRepository);
            DSpotUtils.initClassLoader(this.program, configuration);
            DSpotCompiler.buildCompiler(this.program, true);
            DSpotUtils.compileTests(this.program, mavenHome, mavenLocalRepository);
            InitUtils.initLogLevel(configuration);

        } catch (Exception | InvalidSdkException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reset() {
        this.currentClassTestToBeAmplified = null;
        this.currentMutantsKilledPerTestCase.clear();
        this.testAlreadyRun.clear();
        this.testAlreadyAdded.clear();
        this.originalPitResults.clear();
        this.testAlreadyRun.clear();
        this.testAlreadyAdded.clear();
        this.originalPitResults.clear();
    }

    @Override
    public List<CtMethod> selectToAmplify(List<CtMethod> testsToBeAmplified) {
        if (this.currentClassTestToBeAmplified == null && !testsToBeAmplified.isEmpty()) {
            this.currentClassTestToBeAmplified = testsToBeAmplified.get(0).getDeclaringType();
            this.originalPitResults = PitRunner.run(this.program, this.configuration, this.currentClassTestToBeAmplified)
                    .stream()
                    .filter(result -> result.getStateOfMutant() == PitResult.State.KILLED)
                    .collect(Collectors.toList());
        }
        return testsToBeAmplified;
    }

    @Override
    public List<CtMethod> selectToKeep(List<CtMethod> amplifiedTestToBeKept) {
        if (amplifiedTestToBeKept.isEmpty()) {
            return amplifiedTestToBeKept;
        }
        long time = System.currentTimeMillis();
        CtType clone = this.currentClassTestToBeAmplified.clone();
        clone.setParent(this.currentClassTestToBeAmplified.getParent());
        ((Set<CtMethod>) this.currentClassTestToBeAmplified.getMethods()).forEach(clone::removeMethod);
        amplifiedTestToBeKept.forEach(clone::addMethod);

        try {
            PrintClassUtils.printJavaFile(new File(this.program.getAbsoluteTestSourceCodeDir()), clone);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<PitResult> results = PitRunner.run(this.program, this.configuration, clone);
        List<CtMethod> selectedTests = new ArrayList<>();

        if (results != null) {
            results.stream()
                    .filter(result -> result.getStateOfMutant() == PitResult.State.KILLED)
                    .filter(result -> !this.originalPitResults.contains(result))
                    .forEach(result -> {
                        if (!this.currentMutantsKilledPerTestCase.containsKey(result.getTestCaseMethod())) {
                            this.currentMutantsKilledPerTestCase.put(result.getTestCaseMethod(), new ArrayList<>());
                        }
                        this.currentMutantsKilledPerTestCase.get(result.getTestCaseMethod()).add(result);
                        selectedTests.add(result.getTestCaseMethod());
                    });
        }
        Log.debug("Time to run pit mutation coverage {} ms", System.currentTimeMillis() - time);

        try {
            PrintClassUtils.printJavaFile(new File(this.program.getAbsoluteTestSourceCodeDir()), this.currentClassTestToBeAmplified);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.nbOfTotalMutantKilled += this.currentMutantsKilledPerTestCase.keySet()
                .stream()
                .map(this.currentMutantsKilledPerTestCase::get)
                .reduce(0, (integer, pitResults) -> integer + pitResults.size(), Integer::sum);
        this.testThatKilledMutants.putAll(this.currentMutantsKilledPerTestCase);

        return selectedTests;
    }

    @Override
    public void update() {
        // empty
    }

    @Override
    public void report() {
        final StringBuilder string = new StringBuilder();
        final String nl = System.getProperty("line.separator");

        string.append(nl).append("======= REPORT =======").append(nl);
        string.append("PitMutantScoreSelector: ").append(nl);
        string.append("The original test suite kill ").append(this.originalPitResults.size()).append(" mutants").append(nl);
        string.append("The amplification results with ").append(this.testThatKilledMutants.size()).append(" new tests").append(nl);
        string.append("By amplifying ").append(this.testThatKilledMutants.size()).append(" tests, it kill ")
                .append(this.nbOfTotalMutantKilled).append(" more mutants").append(nl);
        System.out.println(string.toString());

        //intermediate output
        this.testThatKilledMutants.keySet().forEach(amplifiedTest -> {
            string.append(amplifiedTest).append(nl);
            string.append("Kill:").append(nl);
            this.testThatKilledMutants.get(amplifiedTest).forEach(result ->
                    string.append(result).append(nl)
            );
        });

        File reportDir = new File("dspot-report");
        if (!reportDir.exists()) {
            reportDir.mkdir();
        }
        try (FileWriter writer = new FileWriter("dspot-report/pit_mutant_score_selector_report.txt", false)) {
            writer.write(string.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //report json
        reportJSON();
    }

    private void reportJSON() {
        final StringBuilder string = new StringBuilder();

        string.append('{').append(nl);
        List<CtMethod> keys = new ArrayList<>(this.testThatKilledMutants.keySet());

        keys.forEach(amplifiedTest -> {
                    string.append(tab)
                            .append("\"").append(amplifiedTest.getSimpleName()).append("\":{").append(nl)
                            .append(tab).append(tab)
                            .append("\"#AssertionAdded\":").append(Counter.getAssertionOfSinceOrigin(amplifiedTest)).append(",").append(nl)
                            .append(tab).append(tab)
                            .append("\"#InputAdded\":").append(Counter.getInputOfSinceOrigin(amplifiedTest)).append(",").append(nl)
                            .append(tab).append(tab)
                            .append("\"#MutantKilled\":").append(this.testThatKilledMutants.get(amplifiedTest).size()).append(",").append(nl)
                            .append(tab).append(tab)
                            .append("\"MutantsKilled\":[").append(nl)
                            .append(buildListOfIdMutantKilled(amplifiedTest))
                            .append(tab).append(tab).append("]").append(nl)
                            .append(tab).append("}");
                    if (keys.indexOf(amplifiedTest) != keys.size() - 1)
                        string.append(",");
                    string.append(nl);
                }
        );
        string.append("}");

        try (FileWriter writer = new FileWriter("dspot-report/out.json", false)) {
            writer.write(string.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private StringBuilder buildListOfIdMutantKilled(CtMethod amplifiedTest) {
        return this.testThatKilledMutants.get(amplifiedTest).stream()
                .reduce(new StringBuilder(),
                        (builder, element) -> {
                            if (this.testThatKilledMutants.get(amplifiedTest).indexOf(element) ==
                                    this.testThatKilledMutants.get(amplifiedTest).size() - 1) {
                                return builder.append(getLineOfMutantKilled(element));
                            } else {
                                return builder.append(getLineOfMutantKilled(element)).append(",").append(nl);
                            }
                        }, StringBuilder::append);
    }

    private static final String nl = System.getProperty("line.separator");
    private static final String tab = "\t";

    private StringBuilder getLineOfMutantKilled(PitResult element) {
        return new StringBuilder().append(tab).append(tab).append(tab).append("{").append(nl)
                .append(tab).append(tab).append(tab).append(tab)
                .append("\"ID\":").append("\"").append(element.getFullQualifiedNameMutantOperator()).append("\",").append(nl)
                .append(tab).append(tab).append(tab).append(tab)
                .append("\"lineNumber\":\"").append(element.getLineNumber()).append("\",").append(nl)
                .append(tab).append(tab).append(tab).append(tab)
                .append("\"location\":\"").append(element.getLocation()).append("\"").append(nl)
                .append(tab).append(tab).append(tab).append("}").append(nl);
    }

}
