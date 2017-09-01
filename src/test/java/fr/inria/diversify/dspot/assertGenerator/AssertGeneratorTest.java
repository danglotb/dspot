package fr.inria.diversify.dspot.assertGenerator;

import fr.inria.diversify.Utils;
import fr.inria.diversify.buildSystem.android.InvalidSdkException;
import fr.inria.diversify.utils.AmplificationHelper;
import fr.inria.diversify.dspot.AbstractTest;
import fr.inria.stamp.Main;
import org.junit.After;
import org.junit.Test;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

import static org.junit.Assert.assertEquals;


/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 12/8/16
 */
public class AssertGeneratorTest extends AbstractTest {

    @Test
    public void testGenerateAssert() throws Exception, InvalidSdkException {

        /*
            test the generation of assertion
         */

        Main.verbose = true;

        CtClass testClass = Utils.findClass("fr.inria.sample.TestClassWithoutAssert");
        AssertGenerator assertGenerator = new AssertGenerator(Utils.getInputConfiguration(), Utils.getCompiler());
        CtType<?> ctType = AmplificationHelper.createAmplifiedTest(assertGenerator.generateAsserts(testClass), testClass);

        final String expectedBody = "{" + nl  +
                "    fr.inria.sample.ClassWithBoolean cl = new fr.inria.sample.ClassWithBoolean();" + nl  +
                "    // AssertGenerator add assertion" + nl  +
                "    org.junit.Assert.assertTrue(((fr.inria.sample.ClassWithBoolean)cl).getBoolean());" + nl  +
                "    // AssertGenerator add assertion" + nl  +
                "    org.junit.Assert.assertTrue(((fr.inria.sample.ClassWithBoolean)cl).getTrue());" + nl  +
                "    // AssertGenerator add assertion" + nl  +
                "    org.junit.Assert.assertFalse(((fr.inria.sample.ClassWithBoolean)cl).getFalse());" + nl  +
                "    cl.getFalse();" + nl  +
                "    cl.getBoolean();" + nl  +
                "    java.io.File file = new java.io.File(\"\");" +nl +
                "    boolean var = cl.getTrue();" + nl  +
                "}";

        assertEquals(expectedBody, ((CtMethod)ctType.getMethodsByName("test1").stream().findFirst().get()).getBody().toString());
    }

    private static String nl = System.getProperty("line.separator");
}
