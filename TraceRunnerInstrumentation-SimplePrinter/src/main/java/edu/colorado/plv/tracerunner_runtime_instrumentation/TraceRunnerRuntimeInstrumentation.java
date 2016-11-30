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



    static ExecutorService executorService = Executors.newFixedThreadPool(1);
    public static void logCallin(String signature, String methodName, Object[] arguments, Object caller){
        //called on Activity Thread
        int id = count.getAndIncrement();
        long threadID = Thread.currentThread().getId();

        String data = signature + " " + methodName + " thread: " + threadID + " logID: " + id + caller.getClass().getName();
        for(Object argument : arguments){
            if(argument != null)
                data += ":" + argument.getClass().getName();
            else
                data += ":null";
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                count.getAndIncrement();
            }
        };
        executorService.execute(new LogDat(data));
        executorService.execute(r);
    }
    public static void logCallback(String signature, String methodName, Object[] arguments){

    }
    public static void setupNetwork(){
        try {
            socket =
                    new Socket(hostName, portNumber);
            printWriter = new PrintWriter(socket.getOutputStream(), true);
        }catch(IOException e){
            throw new RuntimeException("TraceRunnerInstrumentation failed to open socket");
        }
    }
}

