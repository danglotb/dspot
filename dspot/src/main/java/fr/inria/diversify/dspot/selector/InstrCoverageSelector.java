package fr.inria.diversify.dspot.selector;

import fr.inria.diversify.automaticbuilder.AutomaticBuilderFactory;
import fr.inria.diversify.dspot.support.DSpotCompiler;
import fr.inria.diversify.utils.AmplificationChecker;
import fr.inria.diversify.utils.AmplificationHelper;
import fr.inria.diversify.utils.DSpotUtils;
import fr.inria.diversify.utils.sosiefier.InputConfiguration;
import fr.inria.stamp.EntryPoint;
import fr.inria.stamp.runner.coverage.CoverageListener;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 20/12/17
 */
public class InstrCoverageSelector extends TakeAllSelector {

    private Map<CtMethod<?>, List<CoverageListener.Coverage>> originalResults;

    @Override
    public void init(InputConfiguration configuration) {
        super.init(configuration);
    }

    @Override
    public List<CtMethod<?>> selectToAmplify(List<CtMethod<?>> testsToBeAmplified) {
        if (this.currentClassTestToBeAmplified == null && !testsToBeAmplified.isEmpty()) {
            this.currentClassTestToBeAmplified = testsToBeAmplified.get(0).getDeclaringType();
            String classpath = AutomaticBuilderFactory.getAutomaticBuilder(configuration)
                    .buildClasspath(program.getProgramDir()) + AmplificationHelper.PATH_SEPARATOR +
                    "target/dspot/dependencies/" + AmplificationHelper.PATH_SEPARATOR +
                    program.getProgramDir() + program.getClassesDir() + AmplificationHelper.PATH_SEPARATOR +
                    program.getProgramDir() + program.getTestClassesDir();


            final Map<String, List<CoverageListener.Coverage>> instructionsCoveragePerLinePerTestCasesName =
                    EntryPoint.runCoverage(classpath,
                            program.getProgramDir() + program.getClassesDir() + AmplificationHelper.PATH_SEPARATOR +
                                    program.getProgramDir() + program.getTestClassesDir(),
                            this.currentClassTestToBeAmplified.getQualifiedName()
                    ).getInstructionsCoveragePerLinePerTestCasesName();

            originalResults = mapNameMethodToCtMethod(
                    instructionsCoveragePerLinePerTestCasesName,
                    this.currentClassTestToBeAmplified.getMethods()
            );
        }
        return testsToBeAmplified;
    }

    @Override
    public List<CtMethod<?>> selectToKeep(List<CtMethod<?>> amplifiedTestToBeKept) {

        if (amplifiedTestToBeKept.isEmpty()) {
            return amplifiedTestToBeKept;
        }

        CtType clone = this.currentClassTestToBeAmplified.clone();
        clone.setParent(this.currentClassTestToBeAmplified.getParent());
        this.currentClassTestToBeAmplified.getMethods().stream()
                .filter(AmplificationChecker::isTest)
                .forEach(clone::removeMethod);
        amplifiedTestToBeKept.forEach(clone::addMethod);

        DSpotUtils.printJavaFileWithComment(clone, new File(DSpotCompiler.pathToTmpTestSources));
        final String classpath = AutomaticBuilderFactory
                .getAutomaticBuilder(this.configuration)
                .buildClasspath(this.program.getProgramDir())
                + AmplificationHelper.PATH_SEPARATOR +
                this.program.getProgramDir() + "/" + this.program.getClassesDir()
                + AmplificationHelper.PATH_SEPARATOR + "target/dspot/dependencies/"
                + AmplificationHelper.PATH_SEPARATOR +
                this.program.getProgramDir() + "/" + this.program.getTestClassesDir();

        DSpotCompiler.compile(DSpotCompiler.pathToTmpTestSources, classpath,
                new File(this.program.getProgramDir() + "/" + this.program.getTestClassesDir()));

        final Map<String, List<CoverageListener.Coverage>> instructionsCoveragePerLinePerTestCasesName =
                EntryPoint.runCoverage(classpath,
                        program.getProgramDir() + program.getClassesDir() + AmplificationHelper.PATH_SEPARATOR +
                                program.getProgramDir() + program.getTestClassesDir(),
                        this.currentClassTestToBeAmplified.getQualifiedName()
                ).getInstructionsCoveragePerLinePerTestCasesName();

        final Map<CtMethod<?>, List<CoverageListener.Coverage>> ctMethodListMap =
                mapNameMethodToCtMethod(
                        instructionsCoveragePerLinePerTestCasesName,
                        amplifiedTestToBeKept
                );

        final List<CtMethod<?>> selectedAmplifiedTest = ctMethodListMap.keySet()
                .stream()
                .filter(amplifiedTest ->
                        CoverageListener.isIncreasingNumberOfInstructionExecuted(
                                ctMethodListMap.get(amplifiedTest), originalResults.get(AmplificationHelper.getTopParent(amplifiedTest)))
                ).collect(Collectors.toList());

        this.selectedAmplifiedTest.addAll(selectedAmplifiedTest);

        return selectedAmplifiedTest;
    }

    private Map<CtMethod<?>, List<CoverageListener.Coverage>> mapNameMethodToCtMethod(
            Map<String, List<CoverageListener.Coverage>> mapToBeConverted,
            Collection<CtMethod<?>> testMethods) {
        return mapToBeConverted.keySet().stream()
                .collect(Collectors.toMap(
                        key -> testMethods.stream()
                                .filter(ctMethod -> ctMethod.getSimpleName().equals(key))
                                .findFirst()
                                .get(),
                        mapToBeConverted::get
                ));
    }

}
