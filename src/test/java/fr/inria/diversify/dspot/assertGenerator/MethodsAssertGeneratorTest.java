package fr.inria.diversify.dspot.assertGenerator;

import fr.inria.diversify.Utils;
import fr.inria.diversify.buildSystem.android.InvalidSdkException;
import fr.inria.diversify.dspot.AbstractTest;
import fr.inria.stamp.Main;
import org.junit.Test;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;

import java.util.*;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 3/4/17
 */
public class MethodsAssertGeneratorTest extends AbstractTest {



    @Test
    public void testBuildAssertOnSpecificCases() throws Exception {
        CtClass testClass = Utils.findClass("fr.inria.sample.TestClassWithSpecificCaseToBeAsserted");
        MethodsAssertGenerator mag = new MethodsAssertGenerator(testClass, Utils.getInputConfiguration(), Utils.getCompiler());
        CtMethod test1 = Utils.findMethod("fr.inria.sample.TestClassWithSpecificCaseToBeAsserted", "test1");
        List<CtMethod<?>> test1_buildNewAssert = mag.generateAsserts(testClass, Collections.singletonList(test1));

        final String expectedBody = "{" + nl  +
				"    int a = 0;" + nl  +
				"    int b = 1;" + nl  +
				"    // AssertGenerator create local variable with return value of invocation" + nl  +
				"    int o_test1__3 = new java.util.Comparator<java.lang.Integer>() {" + nl  +
				"        @java.lang.Override" + nl  +
				"        public int compare(java.lang.Integer integer, java.lang.Integer t1) {" + nl  +
				"            return integer - t1;" + nl  +
				"        }" + nl  +
				"    }.compare(a, b);" + nl  +
				"    // AssertGenerator add assertion" + nl  +
				"    org.junit.Assert.assertEquals(-1, ((int) (o_test1__3)));" + nl  +
				"}";

        assertEquals(expectedBody, test1_buildNewAssert.get(0).getBody().toString());
    }

    @Test
    public void testBuildNewAssert() throws InvalidSdkException, Exception {
		Main.verbose = true;
        CtClass testClass = Utils.findClass("fr.inria.sample.TestClassWithoutAssert");
        MethodsAssertGenerator mag = new MethodsAssertGenerator(testClass, Utils.getInputConfiguration(), Utils.getCompiler());

        String nl = System.getProperty("line.separator");

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
				"    java.io.File file = new java.io.File(\"\");" + nl +
				"    boolean var = cl.getTrue();" + nl  +
				"}";

        CtMethod test1 = Utils.findMethod("fr.inria.sample.TestClassWithoutAssert", "test1");

        List<CtMethod<?>> test1_buildNewAssert = mag.generateAsserts(testClass, Collections.singletonList(test1));

        assertEquals(expectedBody, test1_buildNewAssert.get(0).getBody().toString());
    }

}
