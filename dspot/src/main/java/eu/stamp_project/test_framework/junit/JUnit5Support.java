package eu.stamp_project.test_framework.junit;

/**
 * created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 07/11/18
 */
public class JUnit5Support extends JUnitSupport {

    public JUnit5Support() {
        super("org.junit.jupiter.api.Assertions");
    }

    @Override
    protected String getFullQualifiedNameOfAnnotationTest() {
        return "org.junit.jupiter.api.Test";
    }

    @Override
    protected String getFullQualifiedNameOfAnnotationIgnore() {
        return "org.junit.jupiter.api.Disabled";
    }

}
