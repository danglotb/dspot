package eu.stamp_project.test_framework.junit;

import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

/**
 * created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 07/11/18
 */
public class JUnit3Support extends JUnitSupport {

    protected final String qualifiedNameOfAssertClass;

    public JUnit3Support() {
        super("junit.framework.TestCase");
        this.qualifiedNameOfAssertClass = "junit.framework.TestCase";
    }

    @Override
    protected String getFullQualifiedNameOfAnnotationTest() {
        return "";
    }

    @Override
    protected String getFullQualifiedNameOfAnnotationIgnore() {
        return "";
    }

    /*
        For JUnit3, a test method starts by test, otherwise we consider ignored
     */
    @Override
    protected boolean isIgnored(CtMethod<?> candidate) {
        return !candidate.getSimpleName().startsWith("test");
    }

    /*
        For JUnit3, a test method starts by test.
     */
    @Override
    protected boolean isATest(CtMethod<?> candidate) {
        // check that the current test class inherit from TestCase
        final CtType<?> testClass = candidate.getParent(CtType.class);
        if (testClass == null) {
            return false;
        }
        final CtTypeReference<?> superclassReference = testClass.getSuperclass();
        if (superclassReference == null) {
            return false;
        }
        return superclassReference.getQualifiedName().equals(this.qualifiedNameOfAssertClass) &&
                candidate.getAnnotations().isEmpty() && candidate.getSimpleName().startsWith("test");
    }

    @Override
    public CtInvocation<?> buildInvocationToAssertion() {
        return null;
    }
}
