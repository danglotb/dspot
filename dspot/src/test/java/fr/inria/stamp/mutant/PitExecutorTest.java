package fr.inria.stamp.mutant;

import fr.inria.diversify.Utils;
import fr.inria.diversify.mutant.pit.PitResult;
import fr.inria.diversify.mutant.pit.PitResultParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 15/12/17
 */
public class PitExecutorTest {

    @Before
    public void setUp() throws Exception {
        Utils.reset();
        Utils.init("src/test/resources/test-projects/test-projects.properties");
    }

    @Test
    public void testExecute() throws Exception {

        /*
            Test the execution of pit using a given test class
         */

        PitExecutor.execute(Utils.getInputConfiguration(), "example.TestSuiteExample");
        final List<PitResult> pitResults = PitResultParser.parseAndDelete(Utils.getInputProgram().getProgramDir() + "target/report-pits/");
        assertEquals(25, pitResults.size());
    }

    @Test
    public void testExecuteAll() throws Exception {

        /*
            test that when the full qualified name of the test class to be run is null,
                pit executed the whole test suite, i.e. there is no value for targetTests arguments
                TODO: Weak oracle need improvement
         */

        PitExecutor.execute(Utils.getInputConfiguration(), null);
        final List<PitResult> pitResults = PitResultParser.parseAndDelete(Utils.getInputProgram().getProgramDir() + "target/report-pits/");
        assertEquals(25, pitResults.size());
    }

    @After
    public void tearDown() throws Exception {
        Utils.reset();
    }
}
