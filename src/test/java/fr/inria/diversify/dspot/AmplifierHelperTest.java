package fr.inria.diversify.dspot;

import fr.inria.diversify.Utils;
import fr.inria.diversify.runner.InputProgram;
import org.junit.Test;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 1/30/17
 */
public class AmplifierHelperTest {



    @Test
    public void testCreateAmplifiedTestClass() throws Exception {

        Utils.init("src/test/resources/sample/sample.properties");

        CtClass<Object> classTest = Utils.getFactory().Class().get("fr.inria.helper.ClassWithInnerClass");
        List<CtMethod<?>> fakeAmplifiedMethod = classTest.getMethods()
                .stream()
                .map(CtMethod::clone)
                .collect(Collectors.toList());
        fakeAmplifiedMethod.forEach(ctMethod -> ctMethod.setSimpleName("ampl" + ctMethod.getSimpleName()));

        CtType amplifiedTest = AmplificationHelper.createAmplifiedTest(fakeAmplifiedMethod, classTest);
        assertEquals(classTest.getMethods().size() * 2, amplifiedTest.getMethods().size());

        assertFalse(classTest.getElements(new TypeFilter<CtTypeReference>(CtTypeReference.class) {
            @Override
            public boolean matches(CtTypeReference element) {
                return classTest.equals(element.getDeclaration()) &&
                        super.matches(element);
            }
        }).isEmpty());

        assertTrue(amplifiedTest.getElements(new TypeFilter<CtTypeReference>(CtTypeReference.class) {
            @Override
            public boolean matches(CtTypeReference element) {
                return classTest.equals(element.getDeclaration()) &&
                        super.matches(element);
            }
        }).isEmpty());
    }
}
