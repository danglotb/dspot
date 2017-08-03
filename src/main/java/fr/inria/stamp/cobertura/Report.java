package fr.inria.stamp.cobertura;

import net.sourceforge.cobertura.reporting.ReportMain;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 03/08/17
 */
public class Report {

	public static void main(String[] args) {

		try {
			FileUtils.deleteDirectory(new File("test-projects/target/cobertura-report/"));
		} catch (IOException ignored) {

		}

		String[] reportsArgs = new String[]{
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

		System.out.println("... END REPORTING ...");
	}

}
