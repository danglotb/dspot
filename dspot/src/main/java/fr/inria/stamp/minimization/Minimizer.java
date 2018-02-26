package fr.inria.stamp.minimization;

import spoon.reflect.declaration.CtMethod;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 26/02/18
 */
public interface Minimizer {

    /**
     * this Method meant to remove all useless statement according to a given test criterion
     * @param amplifiedTestToBeMinimized
     * @return
     */
    CtMethod<?> minimize(CtMethod<?> amplifiedTestToBeMinimized);

}
