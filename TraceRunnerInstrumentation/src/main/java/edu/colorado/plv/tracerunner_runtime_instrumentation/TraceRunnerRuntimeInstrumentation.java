package edu.colorado.plv.tracerunner_runtime_instrumentation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.RuntimeException;


/**
 * Created by s on 10/3/16.
 */
public class TraceRunnerRuntimeInstrumentation {
    static String hostName = "localhost";
    static int portNumber = 5050;
    static Socket socket = null;
    static PrintWriter printWriter = null;
    static AtomicInteger count = new AtomicInteger(0);


    public static void setupNetwork(){ //TODO: can't even open on main thread or NetworkOnMainThreadException
        try {
            socket = new Socket(hostName, portNumber);

        }catch(IOException e){
            throw new RuntimeException("TraceRunnerInstrumentation failed to open socket");
        }
    }
    static ExecutorService executorService = Executors.newFixedThreadPool(2);
    public static void logCallin(String signature){
        if(socket == null || printWriter == null){
            setupNetwork();
        }
        executorService.execute(new LogDat(printWriter, signature));
        //TODO: implementation
    }
}

