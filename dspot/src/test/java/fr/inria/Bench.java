package fr.inria;

import fr.inria.diversify.Utils;
import fr.inria.diversify.automaticbuilder.AutomaticBuilderFactory;
import fr.inria.stamp.Main;
import fr.inria.stamp.mutant.PitExecutor;
import org.junit.Test;

import java.util.Arrays;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 15/12/17
 */
public class Bench {

    @Test
    public void testBench() throws Exception {
        Main.verbose = true;
        Utils.reset();
        Utils.init("/home/bdanglot/workspace/xwiki-commons/xwiki-commons-core/xwiki-commons-xml/dspot.properties");
        long [] timeMavens = new long[5];
        for (int i = 0 ; i < 5 ; i++) {
            long l = System.currentTimeMillis();
            AutomaticBuilderFactory.getAutomaticBuilder(Utils.getInputConfiguration()).runPit(Utils.getInputProgram().getProgramDir());
            timeMavens[i] = System.currentTimeMillis() - l;
        }
        long [] timeAPIs = new long[5];
        for (int i = 0 ; i < 5 ; i++) {
            long l = System.currentTimeMillis();
            PitExecutor.execute(Utils.getInputConfiguration(), null);
            timeAPIs[i] = System.currentTimeMillis() - l;
        }
        System.out.println(Arrays.stream(timeMavens).reduce((left, right) -> left + right).getAsLong() / 5);
        System.out.println(Arrays.stream(timeAPIs).reduce((left, right) -> left + right).getAsLong() / 5);
        Main.verbose = false;
    }
}
