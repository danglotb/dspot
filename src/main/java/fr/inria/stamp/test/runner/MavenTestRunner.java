package fr.inria.stamp.test.runner;

import fr.inria.diversify.automaticbuilder.AutomaticBuilder;
import fr.inria.diversify.automaticbuilder.AutomaticBuilderFactory;
import fr.inria.diversify.runner.InputConfiguration;
import fr.inria.stamp.test.listener.TestListener;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 16/10/17
 */
public class MavenTestRunner extends TestRunnerAdapter {

    // TODO this class should be called "BuilderTestRunner" and use the builder Factory to retrieve the right builder

    private AutomaticBuilder builder;

    private String pathToProject;

    MavenTestRunner(InputConfiguration configuration) {
        this.builder = AutomaticBuilderFactory.getAutomaticBuilder(configuration);
        pathToProject = configuration.getInputProgram().getProgramDir();
    }

    @Override
    public TestListener run(String fullQualifiedName, Collection<String> testMethodNames) {
        try {
            return this.builder
                    .runTest(pathToProject,
                            fullQualifiedName,
                            testMethodNames.toArray(new String[0])
                    );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TestListener run(String fullQualifiedName, String testMethodName) {
        return this.run(fullQualifiedName, Collections.singleton(testMethodName));
    }

    @Override
    public TestListener run(String fullQualifiedName) {
        return this.run(fullQualifiedName, Collections.emptySet());
    }
}
