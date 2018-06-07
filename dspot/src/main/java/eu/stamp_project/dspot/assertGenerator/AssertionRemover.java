package eu.stamp_project.dspot.assertGenerator;

import eu.stamp_project.utils.AmplificationHelper;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtStatementList;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.reflect.visitor.Filter;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 24/11/17
 */
@SuppressWarnings("unchecked")
public class AssertionRemover {

    private final int[] counter = new int[]{0};

    private Map<CtMethod<?>, List<CtLocalVariable<?>>> variableAssertedPerTestMethod = new HashMap<>();

    public Map<CtMethod<?>, List<CtLocalVariable<?>>> getVariableAssertedPerTestMethod() {
        return variableAssertedPerTestMethod;
    }

    /**
     * Removes all assertions from a test.
     *
     * @param testMethod Test method
     * @return Test's clone without any assertion
     */
    public CtMethod<?> removeAssertion(CtMethod<?> testMethod) {
        CtMethod<?> testWithoutAssertion = AmplificationHelper.cloneTestMethodNoAmp(testMethod);
        variableAssertedPerTestMethod.put(testWithoutAssertion,
                testWithoutAssertion
                        .getElements(AmplificationHelper.ASSERTIONS_FILTER)
                        .stream()
                        .flatMap(invocation -> this.removeAssertion(invocation).stream())
                        .collect(Collectors.toList())
        );
        return testWithoutAssertion;
    }

    /**
     * Replaces an invocation with its arguments.
     *
     * @param invocation Invocation
     */
    public List<CtLocalVariable<?>> removeAssertion(CtInvocation<?> invocation) {
        List<CtLocalVariable<?>> variableReadsAsserted = new ArrayList<>();
        final Factory factory = invocation.getFactory();
        final TypeFilter<CtStatement> statementTypeFilter = new TypeFilter<CtStatement>(CtStatement.class) {
            @Override
            public boolean matches(CtStatement element) {
                return element.equals(invocation);
            }
        };
        for (CtExpression<?> argument : invocation.getArguments()) {
            CtExpression clone = ((CtExpression) argument).clone();
            if (clone instanceof CtUnaryOperator) {
                clone = ((CtUnaryOperator) clone).getOperand();
            }
            if (clone instanceof CtStatement) {
                clone.getTypeCasts().clear();
                invocation.getParent(CtStatementList.class).insertBefore(statementTypeFilter, (CtStatement) clone);
            } else if (!(clone instanceof CtLiteral || clone instanceof CtVariableRead)) {
                CtTypeReference<?> typeOfParameter = clone.getType();
                if (clone.getType().equals(factory.Type().NULL_TYPE)) {
                    typeOfParameter = factory.Type().createReference(Object.class);
                }
                final CtLocalVariable localVariable = factory.createLocalVariable(
                        typeOfParameter,
                        typeOfParameter.getSimpleName() + "_" + counter[0]++,
                        clone
                );
                invocation.getParent(CtStatementList.class).insertBefore(statementTypeFilter, localVariable);
            } else if (clone instanceof CtVariableRead && !(clone instanceof CtFieldRead)) {
                final CtVariableReference variable = ((CtVariableRead) clone).getVariable();
                variableReadsAsserted.add(invocation.getParent(CtBlock.class).getElements(
                        (Filter<CtLocalVariable>) localVariable ->
                                localVariable.getReference().equals(variable)
                ).get(0));
            }
        }
        // must find the first statement list to remove the invocation from it, e.g. the block that contains the assertions
        // the assertion can be inside other stuff, than directly in the block
        CtElement topStatement = invocation;
        while (!(topStatement.getParent() instanceof CtStatementList)) {
            topStatement = topStatement.getParent();
        }
        ((CtStatementList) topStatement.getParent()).removeStatement((CtStatement) topStatement);
        return variableReadsAsserted;
    }

}
