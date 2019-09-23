package eu.stamp_project;

import eu.stamp_project.automaticbuilder.maven.DSpotPOMCreator;
import eu.stamp_project.dspot.input_ampl_distributor.InputAmplDistributor;
import eu.stamp_project.utils.collector.CollectorConfig;
import eu.stamp_project.dspot.DSpot;
import eu.stamp_project.utils.compilation.DSpotCompiler;
import eu.stamp_project.utils.options.JSAPOptions;
import eu.stamp_project.utils.report.output.Output;
import eu.stamp_project.dspot.DSpot;
import eu.stamp_project.utils.options.InputConfiguration;
import eu.stamp_project.utils.RandomHelper;
import eu.stamp_project.utils.report.GlobalReport;
import eu.stamp_project.utils.report.error.ErrorReportImpl;
import eu.stamp_project.utils.report.output.OutputReportImpl;
import eu.stamp_project.utils.report.output.selector.TestSelectorReportImpl;
import eu.stamp_project.utils.test_finder.TestFinder;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.declaration.CtType;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Benjamin DANGLOT benjamin.danglot@inria.fr on 2/9/17
 */
public class Main {

	public static final GlobalReport GLOBAL_REPORT =
			new GlobalReport(new OutputReportImpl(), new ErrorReportImpl(), new TestSelectorReportImpl());

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		try {
			FileUtils.forceDelete(new File("target/dspot/"));
		} catch (Exception ignored) {

		}
//		JSAPOptions.parse(args);
		run();
	}

	public static void run() {
		DSpot dspot = new DSpot(
				InputConfiguration.get().getNbIteration(),
				InputConfiguration.get().getAmplifiers(),
				InputConfiguration.get().getSelector(),
				InputConfiguration.get().getBudgetizerEnum()
		);
		RandomHelper.setSeedRandom(InputConfiguration.get().getSeed());
		createOutputDirectories();
		final long startTime = System.currentTimeMillis();

		final TestFinder testFinder = new TestFinder(
				Arrays.stream(InputConfiguration.get().getExcludedClasses().split(",")).collect(Collectors.toList()),
				Arrays.stream(InputConfiguration.get().getExcludedTestCases().split(",")).collect(Collectors.toList())
		);
		final DSpotCompiler compiler = DSpotCompiler.createDSpotCompiler(
				InputConfiguration.get(),
				InputConfiguration.get().getDependencies()
		);
		InputConfiguration.get().setFactory(compiler.getLauncher().getFactory());

		final List<CtType<?>> testClassesToBeAmplified = testFinder.findTestClasses(InputConfiguration.get().getTestClasses());
		final List<String> testMethodsToBeAmplifiedNames = InputConfiguration.get().getTestCases();
		final InputAmplDistributor inputAmplDistributor = InputConfiguration.get().getBudgetizer().getInputAmplDistributor();
		Output output = new Output(InputConfiguration.get().getAbsolutePathToProjectRoot(), InputConfiguration.get().getOutputDirectory());
		final DSpot dspot = new DSpot(
				testFinder,
				compiler,
				InputConfiguration.get().getSelector(),
				inputAmplDistributor,
				output,
				InputConfiguration.get().getNbIteration(),
				InputConfiguration.get().shouldGenerateAmplifiedTestClass()
		);

		final List<CtType<?>> amplifiedTestClasses = dspot.amplify(testClassesToBeAmplified, testMethodsToBeAmplifiedNames);
		LOGGER.info("Amplification {}.", amplifiedTestClasses.isEmpty() ? "failed" : "succeed");
		final long elapsedTime = System.currentTimeMillis() - startTime;
		LOGGER.info("Elapsed time {} ms", elapsedTime);
		// global report handling
		Main.GLOBAL_REPORT.output();
		Main.GLOBAL_REPORT.reset();
		DSpotPOMCreator.delete();
		// Send info collected.
		InputConfiguration.get().getCollector().sendInfo();
	}

	public static void createOutputDirectories() {
		final File outputDirectory = new File(InputConfiguration.get().getOutputDirectory());
		try {
			if (InputConfiguration.get().shouldClean() && outputDirectory.exists()) {
				FileUtils.forceDelete(outputDirectory);
			}
			if (!outputDirectory.exists()) {
				FileUtils.forceMkdir(outputDirectory);
			}
		} catch (IOException ignored) {
			// ignored
		}
	}

}