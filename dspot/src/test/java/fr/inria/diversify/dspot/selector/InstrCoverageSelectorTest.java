package fr.inria.diversify.dspot.selector;

import fr.inria.diversify.Utils;
import fr.inria.diversify.dspot.DSpot;
import fr.inria.diversify.dspot.amplifier.TestDataMutator;
import org.junit.Test;
import spoon.reflect.declaration.CtType;

import java.util.Collections;
import java.util.List;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 20/12/17
 */
public class InstrCoverageSelectorTest {

    @Test
    public void test() throws Exception {
        Utils.init("src/test/resources/test-projects/test-projects.properties");
        final DSpot dSpot = new DSpot(Utils.getInputConfiguration(),
                1,
                Collections.singletonList(new TestDataMutator()),
                new InstrCoverageSelector()
        );
        final List<CtType> ctTypes = dSpot.amplifyAllTests();
        System.out.println(ctTypes);
    }
}
