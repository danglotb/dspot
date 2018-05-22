package eu.stamp_project.dspot.amplifier;

import eu.stamp_project.utils.DSpotUtils;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 07/03/18
 */
public class AllLiteralAmplifiers implements Amplifier {

    private List<Amplifier> literalAmplifiers;

    public AllLiteralAmplifiers() {
        this.literalAmplifiers = Arrays.asList(
                new StringLiteralAmplifier(),
                new NumberLiteralAmplifier(),
                new BooleanLiteralAmplifier(),
                new CharLiteralAmplifier()
        );
    }

    @Override
    public List<CtMethod<?>> apply(CtMethod<?> testMethod) {
        return this.literalAmplifiers.stream()
                .flatMap(amplifier -> {
                    final Stream<CtMethod<?>> stream = amplifier.apply(testMethod).stream();
                    DSpotUtils.printProgress(
                            this.literalAmplifiers.indexOf(amplifier),
                            this.literalAmplifiers.size()
                    );
                    return stream;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void reset(CtType testClass) {
        this.literalAmplifiers.forEach(amplifier -> amplifier.reset(testClass));
    }
}
