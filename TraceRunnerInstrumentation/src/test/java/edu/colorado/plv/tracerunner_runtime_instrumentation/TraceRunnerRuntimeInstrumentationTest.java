package edu.colorado.plv.tracerunner_runtime_instrumentation;

import static org.junit.Assert.*;
import org.junit.*;

/**
 * Created by Sergio Mover on 10/14/16.
 */
public class TraceRunnerRuntimeInstrumentationTest {
    @org.junit.Test
    public void logCallin() throws Exception {
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

        TraceRunnerRuntimeInstrumentation.logCallin(signature, methodName, args, caller);
    }

}
