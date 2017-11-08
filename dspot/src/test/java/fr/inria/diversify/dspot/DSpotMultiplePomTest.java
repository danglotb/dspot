package fr.inria.diversify.dspot;

import fr.inria.diversify.utils.sosiefier.InputConfiguration;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import spoon.reflect.declaration.CtType;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertFalse;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 05/05/17
 */
public class DSpotMultiplePomTest {

    @Before
    public void setUp() throws Exception {
        try {
            FileUtils.forceDelete(new File("target/dspot/"));
            FileUtils.forceDelete(new File("target/trash/"));
        } catch (Exception ignored) {

        }
    }

    @Test
    public void testCopyMultipleModuleProject() throws Exception {

        /*
            Contract: DSpot is able to amplify a multi-module project
         */

        final InputConfiguration configuration = new InputConfiguration("src/test/resources/multiple-pom/deep-pom-modules.properties");
        final DSpot dspot = new DSpot(configuration);
        final List<CtType> ctTypes = dspot.amplifyAllTests();
        assertFalse(ctTypes.isEmpty());
    }

    @After
    public void tearDown() throws Exception {
        try {
            FileUtils.forceDelete(new File("target/dspot/"));
            FileUtils.forceDelete(new File("target/trash/"));
        } catch (Exception ignored) {

        }
    }
}
