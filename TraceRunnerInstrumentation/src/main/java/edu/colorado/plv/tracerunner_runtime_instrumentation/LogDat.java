package edu.colorado.plv.tracerunner_runtime_instrumentation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class LogDat implements Runnable{
    private final String data;
    private final String methodName;
//    private final String[] arguments;

    LogDat(String data, String methodName){
        this.data = data;
        this.methodName = methodName;
//        this.arguments = arguments;
    }

    @Override
    public void run() {
        if(TraceRunnerRuntimeInstrumentation.printWriter == null){
            TraceRunnerRuntimeInstrumentation.setupNetwork();
        }
        TraceRunnerRuntimeInstrumentation.printWriter.println(data + " " + methodName);
//        for(int i=0; i<arguments.length; ++i)
//            TraceRunnerRuntimeInstrumentation.printWriter.println(arguments[i]);
    }

}
