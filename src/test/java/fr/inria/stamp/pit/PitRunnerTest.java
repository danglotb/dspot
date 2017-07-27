package fr.inria.stamp.pit;

import org.junit.Test;
import org.pitest.functional.predicate.Predicate;
import org.pitest.mutationtest.config.PluginServices;
import org.pitest.mutationtest.config.ReportOptions;
import org.pitest.mutationtest.tooling.AnalysisResult;
import org.pitest.mutationtest.tooling.EntryPoint;
import org.pitest.testapi.TestGroupConfig;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 26/07/17
 */
public class PitRunnerTest {


	@Test
	public void testPitRunner() throws Exception {

		long time = System.currentTimeMillis();

		final ReportOptions options = new ReportOptions();

		List<File> sources = new ArrayList<>();
		sources.add(new File("home/bdanglot/workspace/dspot/src/test/resources/test-projects/src/main/java"));
		sources.add(new File("home/bdanglot/workspace/dspot/src/test/resources/test-projects/src/test/java"));

		String classpathStr = "/home/bdanglot/workspace/dspot/src/test/resources/test-projects/target/classes/:" +
				"/home/bdanglot/workspace/dspot/src/test/resources/test-projects/target/test-classes/" +
				"/home/bdanglot/.m2/repository/junit/junit/4.11/junit-4.11.jar:" +
				"/home/bdanglot/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar:" +
				"/home/bdanglot/.m2/repository/org/pitest/pitest/1.2.0/pitest-1.2.0.jar:";

		URLClassLoader classLoader = new URLClassLoader(Arrays.stream(classpathStr.split(":"))
				.map(File::new)
				.map(file -> {
							try {
								return file.toURL();
							} catch (MalformedURLException e) {
								throw new RuntimeException(e);
							}
						}
				).toArray(URL[]::new), ClassLoader.getSystemClassLoader());

		final PluginServices plugins = new PluginServices(classLoader);

		options.setCodePaths(Collections.singletonList("/home/bdanglot/workspace/dspot/src/test/resources/test-projects/target/classes"));
		options.setClassPathElements(Arrays.asList(classpathStr.split(":")));
		options.setReportDir("out");
		options.setTargetClasses(Collections.singletonList(new Predicate<String>() {
			@Override
			public Boolean apply(String s) {
				return "example.Example".equals(s);
			}
		}));
		options.setTargetTests(Collections.singletonList(new Predicate<String>() {
			@Override
			public Boolean apply(String s) {
				return "example.TestSuiteExample".equals(s);
			}
		}));
		options.setSourceDirs(sources);
		options.setGroupConfig(new TestGroupConfig());
		options.setVerbose(true);

		final EntryPoint entry = new EntryPoint();
		final AnalysisResult result = entry.execute(
				new File("src/test/resources/test-projects/"),
				options, plugins, Collections.emptyMap()
		);

		System.out.println(System.currentTimeMillis() - time);
	}
}
