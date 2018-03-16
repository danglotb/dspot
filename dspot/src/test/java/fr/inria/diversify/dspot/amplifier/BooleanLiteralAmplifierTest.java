package fr.inria.diversify.dspot.amplifier;

import fr.inria.Utils;
import fr.inria.AbstractTest;
import fr.inria.diversify.utils.AmplificationHelper;
import org.junit.Test;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class BooleanLiteralAmplifierTest extends AbstractTest {

    @Test
    public void testAmplify() throws Exception {
        final String nameMethod = "methodBoolean";
        CtClass<Object> literalMutationClass = Utils.getFactory().Class().get("fr.inria.amp.LiteralMutation");
        AmplificationHelper.setSeedRandom(42L);
        Amplifier mutator = new BooleanLiteralAmplifier();
        CtMethod method = literalMutationClass.getMethod(nameMethod);
        List<CtMethod> mutantMethods = mutator.apply(method);
        assertEquals(1, mutantMethods.size());
    }

}
