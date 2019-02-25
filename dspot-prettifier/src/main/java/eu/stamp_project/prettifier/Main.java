package eu.stamp_project.prettifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.stamp_project.prettifier.code2vec.Code2VecExecutor;
import eu.stamp_project.prettifier.code2vec.Code2VecParser;
import eu.stamp_project.prettifier.code2vec.Code2VecWriter;
import eu.stamp_project.prettifier.minimization.GeneralMinimizer;
import eu.stamp_project.prettifier.options.InputConfiguration;
import eu.stamp_project.prettifier.options.JSAPOptions;
import eu.stamp_project.prettifier.output.PrettifiedTestMethods;
import eu.stamp_project.prettifier.output.report.ReportJSON;
import eu.stamp_project.test_framework.TestFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 11/02/19
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static ReportJSON report = new ReportJSON();

    /*
        Apply the following algorithm:
            1) Minimize the amplified test methods.
            2) Rename local variables
            3) rename the test methods
     */

    public static void main(String[] args) {
        JSAPOptions.parse(args);
        final CtType<?> amplifiedTestClass = loadAmplifiedTestClass();
        final List<CtMethod<?>> prettifiedAmplifiedTestMethods = run(amplifiedTestClass);
        // output now
        PrettifiedTestMethods.output(amplifiedTestClass, prettifiedAmplifiedTestMethods);
        output();
    }

    public static CtType<?> loadAmplifiedTestClass() {
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setNoClasspath(true);
        launcher.addInputResource(InputConfiguration.get().getPathToAmplifiedTestClass());
        launcher.buildModel();
        return launcher.getFactory().Class().getAll().get(0);
    }

    public static List<CtMethod<?>> run(CtType<?> amplifiedTestClass) {
        // 1
        final List<CtMethod<?>> minimizedAmplifiedTestMethods = applyMinimization(
                TestFramework.getAllTest(amplifiedTestClass)
        );
        // 2

        // TODO

        // 3
        /*
        applyCode2Vec(InputConfiguration.get().getPathToRootOfCode2Vec(),
                InputConfiguration.get().getRelativePathToModelForCode2Vec(),
                minimizedAmplifiedTestMethods
        );
        */
        return minimizedAmplifiedTestMethods;
    }

    public static List<CtMethod<?>> applyMinimization(List<CtMethod<?>> amplifiedTestMethodsToBeRenamed) {
        final GeneralMinimizer generalMinimizer = new GeneralMinimizer();
        final List<CtMethod<?>> minimizedAmplifiedTestMethods = amplifiedTestMethodsToBeRenamed.stream()
                .map(generalMinimizer::minimize)
                .collect(Collectors.toList());
        generalMinimizer.updateReport();
        return minimizedAmplifiedTestMethods;

    }

    public static void applyCode2Vec(String pathToRootOfCode2Vec,
                                     String relativePathToModel,
                                     List<CtMethod<?>> amplifiedTestMethodsToBeRenamed) {
        Code2VecWriter writer = new Code2VecWriter(pathToRootOfCode2Vec);
        Code2VecExecutor code2VecExecutor = null;
        try {
            code2VecExecutor = new Code2VecExecutor(pathToRootOfCode2Vec, relativePathToModel);
            for (CtMethod<?> amplifiedTestMethodToBeRenamed : amplifiedTestMethodsToBeRenamed) {
                writer.writeCtMethodToInputFile(amplifiedTestMethodToBeRenamed);
                code2VecExecutor.run();
                final String code2vecOutput = code2VecExecutor.getOutput();
                final String predictedSimpleName = Code2VecParser.parse(code2vecOutput);
                LOGGER.info("Code2Vec predicted {} for {} as new name", predictedSimpleName, amplifiedTestMethodToBeRenamed.getSimpleName());
                amplifiedTestMethodToBeRenamed.setSimpleName(predictedSimpleName);
            }
        } finally {
            if (code2VecExecutor != null) {
                code2VecExecutor.stop();
            }
        }
    }

    public static <T extends Number & Comparable<T>> Double getMedian(List<T> list) {
        Collections.sort(list);
        return list.size() % 2 == 0 ?
                list.stream().skip(list.size() / 2 - 1).limit(2).mapToDouble(value -> new Double(value.toString())).average().getAsDouble() :
                new Double(list.stream().skip(list.size() / 2).findFirst().get().toString());
    }

    public static void output() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final String pathname = eu.stamp_project.utils.program.InputConfiguration.get().getOutputDirectory() + "/report.json";
        LOGGER.info("Output a report in {}", pathname);
        final File file  = new File(pathname);
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(gson.toJson(Main.report));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
