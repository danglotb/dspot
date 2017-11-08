package fr.inria.diversify.utils;

import fr.inria.diversify.utils.sosiefier.AddBlockEverywhereProcessor;
import fr.inria.diversify.utils.sosiefier.BranchCoverageProcessor;
import fr.inria.diversify.utils.sosiefier.InputConfiguration;
import fr.inria.diversify.utils.sosiefier.InputProgram;
import fr.inria.stamp.Main;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.compiler.Environment;
import spoon.processing.Processor;
import spoon.reflect.code.CtComment;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;
import spoon.support.JavaOutputProcessor;
import spoon.support.QueueProcessingManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static fr.inria.diversify.utils.AmplificationHelper.PATH_SEPARATOR;


/**
 * User: Simon
 * Date: 18/05/16
 * Time: 16:10
 */
public class DSpotUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(DSpotUtils.class);

	private static StringBuilder progress = new StringBuilder(60);

	public static void printProgress(int done, int total) {
		char[] workchars = {'|', '/', '-', '\\'};
		String format = "\r%3d%% |%s ]%c";
		int percent = (++done * 100) / total;
		int extrachars = (percent / 2) - progress.length();
		while (extrachars-- > 0) {
			progress.append('=');
		}
		System.out.printf(format, percent, progress,
				workchars[done % workchars.length]);
		if (done == total) {
			System.out.flush();
			System.out.println();
			progress = new StringBuilder(60);
		}
	}

	public static void addBranchLogger(InputProgram inputProgram, Factory factory) {
		try {
			applyProcessor(factory, new AddBlockEverywhereProcessor(inputProgram));
			BranchCoverageProcessor branchCoverageProcessor = new BranchCoverageProcessor(inputProgram, inputProgram.getProgramDir(), true);
			branchCoverageProcessor.setLogger(fr.inria.diversify.logger.Logger.class.getCanonicalName());
			applyProcessor(factory, branchCoverageProcessor);
			copyPackageFromResources(
					"fr/inria/diversify/logger", "ClassObserver",
					"KeyWord", "Logger", "LogWriter", "PathBuilder", "Pool", "ShutdownHookLog");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void printJavaFileWithComment(CtType<?> type, File directory) {
		Factory factory = type.getFactory();
		Environment env = factory.getEnvironment();
		env.setCommentEnabled(true);
		JavaOutputProcessor processor = new JavaOutputProcessor(directory, new DefaultJavaPrettyPrinter(env));
		processor.setFactory(factory);
		processor.createJavaFile(type);
	}

	public static void printAmplifiedTestClass(CtType<?> type, File directory) {
		final String pathname = directory.getAbsolutePath() + "/" + type.getQualifiedName().replaceAll("\\.", "/") + ".java";
		if (new File(pathname).exists()) {
			printJavaFileWithComment(addGeneratedTestToExistingClass(type, pathname), directory);
		} else {
			printJavaFileWithComment(type, directory);
		}
	}

	private static CtClass<?> addGeneratedTestToExistingClass(CtType<?> type, String pathname) {
		Launcher launcher = new Launcher();
		launcher.getEnvironment().setNoClasspath(true);
		launcher.addInputResource(pathname);
		launcher.buildModel();
		final CtClass<?> existingAmplifiedTest = launcher.getFactory().Class().get(type.getQualifiedName());
		type.getMethods().stream()
				.filter(testCase -> !existingAmplifiedTest.getMethods().contains(testCase))
				.forEach(existingAmplifiedTest::addMethod);
		return existingAmplifiedTest;
	}

	public static void printAllClasses(Factory factory, File out) {
		factory.Class().getAll().forEach(type -> printJavaFileWithComment(type, out));
	}

	public static void addComment(CtElement element, String content, CtComment.CommentType type) {
		CtComment comment = element.getFactory().createComment(content, type);
		if (!element.getComments().contains(comment)) {
			element.addComment(comment);
		}
	}

	@Deprecated
	public static String mavenHome;

	@Deprecated
	public static String buildMavenHome(InputConfiguration inputConfiguration) {
		if (mavenHome == null) {
			if (inputConfiguration != null && inputConfiguration.getProperty("maven.home") != null) {
				mavenHome = inputConfiguration.getProperty("maven.home");
			} else {
				if(!setMavenHome(envVariable -> System.getenv().get(envVariable) != null,
						envVariable -> System.getenv().get(envVariable),
						"MAVEN_HOME", "M2_HOME")) {//TODO asking if predefined values are useful or not
					if (!setMavenHome(path -> new File(path).exists(),
							Function.identity(),
							"/usr/share/maven/", "/usr/local/maven-3.3.9/", "/usr/share/maven3/")) {
						throw new RuntimeException("Maven home not found, please set properly MAVEN_HOME or M2_HOME.");
					}
				}
			}
		}
		return mavenHome;
	}

	private static boolean setMavenHome(Predicate<String> conditional, Function<String, String> getFunction, String... possibleValues) {
		Arrays.stream(possibleValues)
				.filter(conditional)
				.findFirst()
				.ifPresent(s -> mavenHome = getFunction.apply(s));
		return mavenHome != null;
	}

	private static void applyProcessor(Factory factory, Processor processor) {
		QueueProcessingManager pm = new QueueProcessingManager(factory);
		pm.addProcessor(processor);
		pm.process(factory.Package().getRootPackage());
	}

	public static final String pathToDSpotDependencies = "target/dspot/dependencies/";

	public static void copyPackageFromResources(String packagePath, String... classToCopy) {
		final String pathToTestClassesDirectory = pathToDSpotDependencies + "/" + packagePath + "/";
		final String directory = packagePath.split("/")[packagePath.split("/").length -1];
		try {
			FileUtils.forceMkdir(new File(pathToTestClassesDirectory));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Arrays.stream(classToCopy).forEach(file -> {
			OutputStream resStreamOut = null;
			try {
				final InputStream resourceAsStream =  DSpotUtils.class.getClassLoader().getResourceAsStream( directory + "/" + file + ".class");
				resStreamOut =
						new FileOutputStream(pathToTestClassesDirectory + file + ".class");
				int readBytes;
				byte[] buffer = new byte[4096];
				while ((readBytes = resourceAsStream.read(buffer)) > 0) {
					resStreamOut.write(buffer, 0, readBytes);
				}
				resStreamOut.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	public static final Function<String, String> shouldAddSeparator = string -> string.endsWith("/") ? "" : "/";

	public static Function<InputConfiguration, String> computeProgramDirectory = configuration ->
			configuration.getProperty("project") + shouldAddSeparator.apply(configuration.getProperty("project")) +
					(configuration.getProperty("targetModule") != null ?
							configuration.getProperty("targetModule") + shouldAddSeparator.apply(configuration.getProperty("project")) : "");

	private static void copyDirectory(String pathToProgramDir, String resourcesToBeCopied, String pathDirectoryToCopy) {
		FileUtils.listFiles(new File(pathToProgramDir + resourcesToBeCopied),
				new FileFileFilter() {
					@Override
					public boolean accept(File file) {
						return !file.getAbsolutePath().endsWith("java");
					}
				},
				new FileFileFilter() {
					@Override
					public boolean accept(File file) {
						return true;
					}
				}
		).forEach(file ->
				DSpotUtils.copyFile(pathToProgramDir, resourcesToBeCopied, pathDirectoryToCopy, file)
		);
	}


	private static void copyFile(String pathToProgramDir, String pathToResourceToBeCopied, String pathDirectoryToCopy, File fileToBeCopied) {
		try {
			if(Main.verbose) {
				LOGGER.info("copy {} to {}", fileToBeCopied.getPath(), pathDirectoryToCopy + "/" +
						fileToBeCopied.getPath().substring(
								pathToProgramDir.length() + pathToResourceToBeCopied.length()
						)
				);
			}
			FileUtils.copyFile(fileToBeCopied,
					new File(pathDirectoryToCopy + "/" +
							fileToBeCopied.getPath().substring(
									pathToProgramDir.length() + pathToResourceToBeCopied.length()
								)
							)
			);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void copyGivenResources(String key, String outputDirectory, InputConfiguration configuration) {
		final InputProgram program = configuration.getInputProgram();
		final String resources = configuration.getProperty(key);
		if (resources != null) {
			Arrays.stream(resources.split(PATH_SEPARATOR)).forEach(resource -> {
				if (new File(program.getProgramDir() + resource).isDirectory()) {
					copyDirectory(program.getProgramDir(),
							resource,
							program.getProgramDir() + outputDirectory);
				} else {
					copyFile(program.getProgramDir(),
							resource,
							program.getProgramDir() + outputDirectory,
							new File(program.getProgramDir() + resource));
				}
			});
		}
	}

	public static void copyResources(InputConfiguration configuration) {
		final InputProgram program = configuration.getInputProgram();
		copyGivenResources("srcResources", program.getClassesDir(), configuration);
		copyGivenResources("testResources", program.getTestClassesDir(), configuration);
	}
}
