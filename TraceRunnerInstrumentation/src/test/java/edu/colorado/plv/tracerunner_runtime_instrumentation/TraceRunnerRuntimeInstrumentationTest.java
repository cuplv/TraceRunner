package edu.colorado.plv.tracerunner_runtime_instrumentation;

import static org.junit.Assert.*;
import org.junit.*;

/**
 * Created by Sergio Mover on 10/14/16.
 */
public class TraceRunnerRuntimeInstrumentationTest {
    private static final long MAX_TO = 5;

    @org.junit.Test
    public void logCallin() throws RuntimeException {
        String signature = "method signature";
        String methodName = "method name";
        Object caller = "caller string";

        /* set the parameters */
        Object[] args = new Object[6];

        int intVar = 1;
        double doubleVar = 2.3;
        String stringVar = "stirng var value";

        args[0] = 1;
        args[1] = intVar;
        args[2] = 2.3;
        args[3] = doubleVar;
        args[4] = stringVar;
        args[5] = "string constant value";

        // TODO the test should start a server side socket to get the data and check the results
        TraceRunnerRuntimeInstrumentation.logCallin(signature, methodName, args); //, caller);
        TraceRunnerRuntimeInstrumentation.logCallin(signature, methodName, args); //, caller);
        TraceRunnerRuntimeInstrumentation.shutdownAndAwaitTermination();
    }

}
