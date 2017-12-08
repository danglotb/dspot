package fr.inria.teardown;

import org.junit.After;

public class WithTearDownTest {

    @After
    public void tearDown() throws Exception {
        System.out.println("tear down method");
    }

}