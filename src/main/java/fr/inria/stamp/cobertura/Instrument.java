package fr.inria.stamp.cobertura;

import net.sourceforge.cobertura.instrument.InstrumentMain;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 03/08/17
 */
public class Instrument {

	public static void main(String[] args) {
		try {
			FileUtils.deleteDirectory(new File("test-projects/target/instrumentation/"));
			FileUtils.forceDelete(new File("cobertura.ser"));
		} catch (IOException ignored) {

		}
		String[] argsInstrument = new String[]{
				"--destination", "test-projects/target/instrumentation/",
				"--basedir", "test-projects/",
				"test-projects/target/classes/"
		};
		InstrumentMain.instrument(argsInstrument);
	}
}
