package eu.stamp_project.dspot.amplifier;

import eu.stamp_project.utils.AmplificationChecker;
import eu.stamp_project.utils.AmplificationHelper;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 18/09/17
 */
public abstract class AbstractLiteralAmplifier<T> implements Amplifier {

    protected CtType<?> testClassToBeAmplified;

    protected final TypeFilter<CtLiteral<T>> LITERAL_TYPE_FILTER = new TypeFilter<CtLiteral<T>>(CtLiteral.class) {
        @Override
        public boolean matches(CtLiteral<T> literal) {
            try {
                /*if ("Amplified".equals(literal.getDocComment())) {
                    return false;
                }*/
                Class<?> clazzOfLiteral = null;
                if ((literal.getParent() instanceof CtInvocation &&
                        AmplificationChecker.isAssert((CtInvocation) literal.getParent()))
                        || isConcatenationOfLiteral(literal)
                        || literal.getParent(CtAnnotation.class) != null) {
                    return false;
                } else if (literal.getValue() == null) {
                    if (literal.getParent() instanceof CtInvocation<?>) {
                        final CtInvocation<?> parent = (CtInvocation<?>) literal.getParent();
                        clazzOfLiteral = parent.getExecutable()
                                .getDeclaration()
                                .getParameters()
                                .get(parent.getArguments().indexOf(literal))
                                .getType()
                                .getActualClass(); // getting the class of the expected parameter
                    } else if (literal.getParent() instanceof CtAssignment) {
                        clazzOfLiteral = ((CtAssignment) literal.getParent())
                                .getAssigned()
                                .getType()
                                .getActualClass(); // getting the class of the assignee
                    } else if (literal.getParent() instanceof CtLocalVariable) {
                        clazzOfLiteral = ((CtLocalVariable) literal.getParent())
                                .getType()
                                .getActualClass(); // getting the class of the local variable
                    }
                } else {
                    clazzOfLiteral = literal.getValue().getClass();
                }
                return getTargetedClass().isAssignableFrom(clazzOfLiteral);
            } catch (Exception e) {
                // maybe need a warning ?
                return false;
            }
        }

        private boolean isConcatenationOfLiteral(CtLiteral<T> literal) {
            CtElement currentElement = literal;
            while (currentElement.getParent() instanceof CtBinaryOperator) {
                currentElement = currentElement.getParent();
            }
            return currentElement.getParent() instanceof CtInvocation &&
                    AmplificationChecker.isAssert((CtInvocation) currentElement.getParent());
        }
    };

    @Override
    public List<CtMethod<?>> apply(CtMethod<?> testMethod) {
        List<CtLiteral<T>> literals = testMethod.getElements(LITERAL_TYPE_FILTER);
        if (literals.isEmpty()) {
            return Collections.emptyList();
        }

        final Optional<CtLiteral<String>> first = testMethod.getElements(
                new TypeFilter<CtLiteral<String>>(CtLiteral.class))
                .stream()
                .filter(stringCtLiteral -> "Amplified".equals(stringCtLiteral.getDocComment()))
                .findFirst();

        if (first.isPresent()) {
            final CtLiteral<String> literal = first.get();
            literals = literals.subList(literals.indexOf(literal) + 1, literals.size());
        }

        return literals.stream()
                .filter(stringCtLiteral -> !"Amplified".equals(stringCtLiteral.getDocComment()))
                .flatMap(literal -> {
                    final Set<T> amplify = this.amplify(literal);
                            return amplify.stream()
                                    .map(newValue -> {
                                        final T originalValue = literal.getValue();
                                        literal.setValue(newValue);
                                        literal.setDocComment("Amplified");
                                        CtMethod<?> clone = AmplificationHelper.cloneTestMethodForAmp(testMethod, getSuffix());
                                        literal.setDocComment("");
                                        literal.setValue(originalValue);
                                        return clone;
                                    });
                        }
                ).collect(Collectors.toList());
    }

    @Override
    public void reset(CtType testClass) {
        AmplificationHelper.reset();
        this.testClassToBeAmplified = testClass;
    }

    protected abstract Set<T> amplify(CtLiteral<T> existingLiteral);

    protected abstract String getSuffix();

    protected abstract Class<?> getTargetedClass();

}
