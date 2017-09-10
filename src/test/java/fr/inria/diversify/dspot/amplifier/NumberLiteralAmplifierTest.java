package fr.inria.diversify.dspot.amplifier;

import fr.inria.diversify.Utils;
import fr.inria.diversify.buildSystem.android.InvalidSdkException;
import fr.inria.diversify.dspot.AbstractTest;
import fr.inria.diversify.utils.AmplificationHelper;
import org.junit.Test;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class NumberLiteralAmplifierTest extends AbstractTest {

    @Test
    public void testIntMutation() throws Exception, InvalidSdkException {
        final String nameMethod = "methodInteger";
        final int originalValue = 23;
        CtClass<Object> literalMutationClass = Utils.getFactory().Class().get("fr.inria.amp.LiteralMutation");
        AmplificationHelper.setSeedRandom(42L);
        NumberLiteralAmplifier amplificator = getAmplifier(literalMutationClass);
        CtMethod method = literalMutationClass.getMethod(nameMethod);
        List<Integer> expectedValues = Arrays.asList(22, 24, 2147483647, -2147483648, 0);

        List<CtMethod> mutantMethods = amplificator.apply(method);
        assertEquals(5, mutantMethods.size());
        for (int i = 0; i < mutantMethods.size(); i++) {
            CtMethod mutantMethod = mutantMethods.get(i);
            assertEquals(nameMethod + "litNum" + (i + 1), mutantMethod.getSimpleName());
            CtLiteral mutantLiteral = mutantMethod.getBody().getElements(new TypeFilter<>(CtLiteral.class)).get(0);
            assertNotEquals(originalValue, mutantLiteral.getValue());
            assertTrue(expectedValues.contains(mutantLiteral.getValue()));
        }
    }

    @Test
    public void testDoubleMutation() throws Exception, InvalidSdkException {
        final String nameMethod = "methodDouble";
        final double originalValue = 23.0D;
        CtClass<Object> literalMutationClass = Utils.getFactory().Class().get("fr.inria.amp.LiteralMutation");
        AmplificationHelper.setSeedRandom(42L);
        NumberLiteralAmplifier amplificator = getAmplifier(literalMutationClass);
        CtMethod method = literalMutationClass.getMethod(nameMethod);
        List<Double> expectedValues = Arrays.asList(22.0D, 24.0D, Double.MAX_VALUE, Double.MIN_VALUE, Double.MIN_NORMAL,
                Double.NaN ,Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY , 0.0D);

        List<CtMethod> mutantMethods = amplificator.apply(method);
        System.out.println(mutantMethods);
        assertEquals(9, mutantMethods.size());
        for (int i = 0; i < mutantMethods.size(); i++) {
            CtMethod mutantMethod = mutantMethods.get(i);
            assertEquals(nameMethod + "litNum" + (i + 1), mutantMethod.getSimpleName());
            CtLiteral mutantLiteral = mutantMethod.getBody().getElements(new TypeFilter<>(CtLiteral.class)).get(0);
            assertNotEquals(originalValue, mutantLiteral.getValue());
            assertTrue(mutantLiteral.getValue() + " not in expected values",
                    expectedValues.contains(mutantLiteral.getValue()));
        }
    }

    private NumberLiteralAmplifier getAmplifier(CtClass<Object> literalMutationClass) {
        NumberLiteralAmplifier amplificator = new NumberLiteralAmplifier();
        amplificator.reset(literalMutationClass);
        return amplificator;
    }

}
