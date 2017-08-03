package fr.inria.stamp.cobertura;

import fr.inria.stamp.test.listener.TestListener;
import fr.inria.stamp.test.runner.DefaultTestRunner;
import net.sourceforge.cobertura.instrument.InstrumentMain;
import net.sourceforge.cobertura.reporting.ReportMain;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 03/08/17
 */
public class CoberturaExecutor {

	public static void main(String[] args) {

		try {
			FileUtils.deleteDirectory(new File("test-projects/target/instrumentation/"));
			FileUtils.deleteDirectory(new File("test-projects/target/cobertura-report/"));
			FileUtils.forceDelete(new File("cobertura.ser"));
		} catch (IOException ignored) {

		}
		String[] argsInstrument = new String[]{
				"--destination", "test-projects/target/instrumentation/",
				"--basedir", "test-projects/",
				"test-projects/target/classes/"
		};
		InstrumentMain.instrument(argsInstrument);

		System.out.println("... END INSTRUMENTATION ...");

		String classpath = "cobertura-2.1.2-SNAPSHOT.jar:" +
				"test-projects/target/instrumentation/:" +
				"test-projects/target/classes/:" +
				"test-projects/target/test-classes/";

		final ClassLoader classLoader = new URLClassLoader(Arrays.stream(classpath.split(":"))
				.map(File::new)
				.map(File::toURI)
				.map(uri -> {
					try {
						return uri.toURL();
					} catch (MalformedURLException e) {
						throw new RuntimeException(e);
					}
				}).toArray(URL[]::new), ClassLoader.getSystemClassLoader());

		System.setProperty("net.sourceforge.cobertura.datafile", "cobertura.ser");

		final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(classLoader);
		final TestListener run = new DefaultTestRunner((classpath)).run("example.TestSuiteExample");
		System.out.println("Passing: " + run.getPassingTests().size());
		System.out.println("Failing: " + run.getFailingTests().size());
		System.out.println("Running: " + run.getRunningTests().size());
		Thread.currentThread().setContextClassLoader(contextClassLoader);

		System.out.println("... END RUN TEST ...");

		/*String[] reportsArgs = new String[]{
				"--format", "html",
				"--datafile", "cobertura.ser",
				"--destination", "test-projects/target/cobertura-report",
				"test-projects/src/main/java/"
		};
		try {
			ReportMain.generateReport(reportsArgs);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		System.out.println("... END REPORTING ...");*/
	}

}
