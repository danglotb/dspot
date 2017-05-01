package fr.inria.diversify.dspot.amplifier;


import fr.inria.diversify.dspot.AmplificationChecker;
import fr.inria.diversify.dspot.AmplificationHelper;
import fr.inria.diversify.dspot.value.Value;
import fr.inria.diversify.dspot.value.ValueFactory;
import fr.inria.diversify.runner.InputProgram;
import fr.inria.diversify.utils.CtTypeUtils;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.Query;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * User: Simon
 * Date: 18/11/16
 * Time: 10:40
 */
public class StatementAdd implements Amplifier {

    private String filter;
    private Set<CtMethod> methods;
    private Map<CtType, Boolean> hasConstructor;
    private ValueFactory valueFactory;
    private Factory factory;
    private final int[] count = {0};

    @Deprecated //The fact that StatementAdd Amplifier need the input program is a conception issue IMHO
    public StatementAdd(InputProgram program) {
        this.valueFactory = new ValueFactory(program);
        this.factory = program.getFactory();
        this.filter = "";
        this.hasConstructor = new HashMap<>();
    }

    public StatementAdd(InputProgram program, String filter) {
        this.valueFactory = new ValueFactory(program);
        this.factory = program.getFactory();
        this.filter = filter;
        this.hasConstructor = new HashMap<>();
    }

    public StatementAdd(Factory factory, ValueFactory valueFactory, String filter) {
        this.valueFactory = valueFactory;
        this.factory = factory;
        this.filter = filter;
        this.hasConstructor = new HashMap<>();

    }

    @Override
    public List<CtMethod> apply(CtMethod method) {
        count[0] = 0;
        List<CtInvocation> invocations = getInvocation(method);
        final List<CtMethod> ampMethods = invocations.stream()
                .filter(invocation -> invocation.getExecutable().getDeclaration() != null &&
                        !((CtMethod) invocation.getExecutable().getDeclaration()).getModifiers().contains(ModifierKind.STATIC))
                .flatMap(invocation ->
                        findMethodsWithTargetType(invocation.getTarget().getType()).stream()
                                .map(addMth -> addInvocation(method, addMth, invocation.getTarget(), invocation, AmplificationHelper.getRandom().nextBoolean()))
                                .collect(Collectors.toList()).stream())
                .collect(Collectors.toList());

        // use the existing invocation to add new invocation

        // use the potential parameters to generate new invocation

        invocations.stream()
                .filter(invocation -> !CtTypeUtils.isPrimitive(invocation.getType()) || !CtTypeUtils.isString(invocation.getType()))
                .forEach(invocation -> {
                    List<CtMethod> methodsWithTargetType = findMethodsWithTargetType(invocation.getType());
                    if (!methodsWithTargetType.isEmpty()) {
                        CtLocalVariable localVar = factory.Code().createLocalVariable(
                                invocation.getType(),
                                "invoc_" + count[0]++,
                                invocation);
                        CtExpression<?> target = createLocalVarRef(localVar);
                        CtMethod methodClone = AmplificationHelper.cloneMethod(method, "");
                        CtStatement stmt = findInvocationIn(methodClone, invocation);
                        stmt.replace(localVar);

                        ampMethods.addAll(methodsWithTargetType.stream()
                                .map(addMth -> addInvocation(methodClone, addMth, target, localVar, false))
                                .collect(Collectors.toList()));
                    }
                });

        //  use the return value of the first generation to generate

        return ampMethods;
    }

    @Override
    public CtMethod applyRandom(CtMethod method) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset(CtType testClass) {
        AmplificationHelper.reset();
        initMethods(testClass);
    }

    private CtMethod addInvocation(CtMethod mth, CtMethod mthToAdd, CtExpression target, CtStatement position, boolean before) {
        CtMethod methodClone = AmplificationHelper.cloneMethod(mth, "_sd");
        CtBlock body = methodClone.getBody();

        List<CtParameter> parameters = mthToAdd.getParameters();
        List<CtExpression<?>> arg = new ArrayList<>(parameters.size());
        for (int i = 0; i < parameters.size(); i++) {
            try {
                CtParameter parameter = parameters.get(i);
                Value value = valueFactory.getValueType(parameter.getType()).getRandomValue();
                if (value != null) {
                    CtLocalVariable localVar = factory.Code().createLocalVariable(
                            generateStaticType(parameter.getType(), value.getDynamicType()),
                            parameter.getSimpleName() + "_" + count[0]++,
                            null);
                    body.getStatements().add(0, localVar);
                    localVar.setParent(body);
                    arg.add(createLocalVarRef(localVar));
                    value.initLocalVar(body, localVar);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        CtExpression targetClone = target.clone();
        CtInvocation newInvocation = factory.Code().createInvocation(targetClone, mthToAdd.getReference(), arg);

        CtStatement stmt = findInvocationIn(methodClone, position);
        if (before) {
            stmt.insertBefore(newInvocation);
        } else {
            stmt.insertAfter(newInvocation);
        }

        return methodClone;
    }

    private CtStatement findInvocationIn(CtMethod method, CtStatement invocation) {
        List<CtStatement> statements = Query.getElements(method, new TypeFilter(CtStatement.class));
        return statements.stream()
                .filter(s -> s.toString().equals(invocation.toString()))
                .findFirst().orElse(null);
    }

    private CtExpression<?> createLocalVarRef(CtLocalVariable var) {
        CtLocalVariableReference varRef = factory.Code().createLocalVariableReference(var);
        CtVariableAccess varRead = factory.Code().createVariableRead(varRef, false);

        return varRead;
    }

    private CtTypeReference generateStaticType(CtTypeReference parameterType, String dynamicTypeName) {
        CtTypeReference type = factory.Core().clone(parameterType);
        type.getActualTypeArguments().clear();

        if ((dynamicTypeName.contains("<") || dynamicTypeName.contains(">"))
                && !(dynamicTypeName.contains("<null") || dynamicTypeName.contains("null>"))) {

            String[] genericTypes = dynamicTypeName.substring(dynamicTypeName.indexOf("<") + 1, dynamicTypeName.length() - 1).split(", ");
            Arrays.stream(genericTypes)
                    .forEach(genericType -> type.getActualTypeArguments().add(factory.Type().createReference(genericType)));
        }
        return type;
    }

    private List<CtMethod> findMethodsWithTargetType(CtTypeReference type) {
        if (type == null) {
            return Collections.emptyList();
        } else {
            return methods.stream()
                    .filter(mth -> mth.getDeclaringType().getReference().getQualifiedName().equals(type.getQualifiedName()))
                    .collect(Collectors.toList());
        }
    }

    private List<CtInvocation> getInvocation(CtMethod method) {
        List<CtInvocation> statements = Query.getElements(method, new TypeFilter(CtInvocation.class));
        return statements.stream()
                .filter(invocation -> invocation.getParent() instanceof CtBlock)
                .filter(stmt -> stmt.getExecutable().getDeclaringType().getQualifiedName().startsWith(filter)) // filter on the name for amplify a specific type
                .collect(Collectors.toList());
    }

    private void initMethods(CtType testClass) {
        methods = AmplificationHelper.computeClassProvider(testClass).stream()
                .flatMap(cl -> {
                    Set<CtMethod<?>> allMethods = cl.getAllMethods();
                    return allMethods.stream();
                })
                .filter(mth -> !mth.getModifiers().contains(ModifierKind.ABSTRACT))//TODO abstract
                .filter(mth -> !mth.getModifiers().contains(ModifierKind.PRIVATE))
                .filter(mth -> mth.getBody() != null)
                .filter(mth -> !mth.getBody().getStatements().isEmpty())
                .filter(mth -> !AmplificationChecker.isTest(mth))
                .filter(mth -> {
                    List<CtParameter<?>> parameters = mth.getParameters();
                    return parameters.stream()
                            .map(CtTypedElement::getType)
                            .allMatch(param -> CtTypeUtils.isPrimitive(param)
                                    || CtTypeUtils.isString(param)
                                    || CtTypeUtils.isPrimitiveArray(param)
                                    || CtTypeUtils.isPrimitiveCollection(param)
                                    || CtTypeUtils.isPrimitiveMap(param)
                                    || isSerializable(param));
                })
                .collect(Collectors.toSet());
    }

    private boolean isSerializable(CtTypeReference type) {
        if (!hasConstructor.containsKey(type.getDeclaration())) {
            if (type.getDeclaration() instanceof CtClass) {
                CtClass cl = (CtClass) type.getDeclaration();
                hasConstructor.put(type.getDeclaration(),
                        cl.isTopLevel() && valueFactory.hasConstructorCall(cl, true));
            } else {
                hasConstructor.put(type.getDeclaration(), false);
            }
        }
        return hasConstructor.get(type.getDeclaration());
    }
}
