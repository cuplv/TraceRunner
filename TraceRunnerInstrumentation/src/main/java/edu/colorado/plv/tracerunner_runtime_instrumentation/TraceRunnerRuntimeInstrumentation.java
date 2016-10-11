package edu.colorado.plv.tracerunner_runtime_instrumentation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by s on 10/3/16.
 */
public class TraceRunnerRuntimeInstrumentation {
    public void hello(){
        System.out.println("");
    }
    static ExecutorService executorService = Executors.newFixedThreadPool(2);
    public static void logCallin(String signature){
        executorService.execute(new Runnable() {
            @Override
            public void run() {

            }
        });
        //TODO: implementation
    }
}
