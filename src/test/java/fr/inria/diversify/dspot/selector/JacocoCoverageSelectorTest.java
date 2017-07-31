package fr.inria.diversify.dspot.selector;

import fr.inria.diversify.buildSystem.android.InvalidSdkException;
import fr.inria.diversify.dspot.DSpot;
import fr.inria.diversify.runner.InputConfiguration;
import org.junit.Test;

import java.util.Collections;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 25/07/17
 */
public class JacocoCoverageSelectorTest {

	@Test
	public void testDSpotWithJacocoCoverageSelector() throws Exception, InvalidSdkException {
		long time = System.currentTimeMillis();
		InputConfiguration configuration = new InputConfiguration("src/test/resources/test-projects/test-projects.properties");
		DSpot dspot = new DSpot(configuration, new JacocoCoverageSelector());
		dspot.amplifyTest("example.TestSuiteExample", Collections.singletonList("test2"));
		System.out.println(System.currentTimeMillis() - time);
	}
	
}