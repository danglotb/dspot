package fr.inria.stamp.test.runner;

import fr.inria.stamp.test.listener.TestListener;
import org.junit.runner.notification.RunListener;

import java.util.Collection;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 16/10/17
 */
public class TestRunnerAdapter implements TestRunner {
    @Override
    public TestListener run(String fullQualifiedName, Collection<String> testMethodNames) {
        throw new UnsupportedOperationException("Must be implemented by inherited classes");
    }

    @Override
    public TestListener run(String fullQualifiedName, String testMethodName) {
        throw new UnsupportedOperationException("Must be implemented by inherited classes");
    }

    @Override
    public TestListener run(String fullQualifiedName) {
        throw new UnsupportedOperationException("Must be implemented by inherited classes");
    }

    @Override
    public TestListener run(Class<?> classTest, Collection<String> testMethodNames) {
        throw new UnsupportedOperationException("Must be implemented by inherited classes");
    }

    @Override
    public TestListener run(Class<?> classTest, String testMethodName) {
        throw new UnsupportedOperationException("Must be implemented by inherited classes");
    }

    @Override
    public TestListener run(Class<?> classTest) {
        throw new UnsupportedOperationException("Must be implemented by inherited classes");
    }

    @Override
    public TestListener run(Class<?> testClass, RunListener listener) {
        throw new UnsupportedOperationException("Must be implemented by inherited classes");
    }

    @Override
    public TestListener run(Class<?> testClass, Collection<String> methodNames, RunListener listener) {
        throw new UnsupportedOperationException("Must be implemented by inherited classes");
    }

    @Override
    public TestListener run(String fullQualifiedName, RunListener listener) {
        throw new UnsupportedOperationException("Must be implemented by inherited classes");
    }

    @Override
    public TestListener run(String fullQualifiedName, Collection<String> methodNames, RunListener listener) {
        throw new UnsupportedOperationException("Must be implemented by inherited classes");
    }
}
