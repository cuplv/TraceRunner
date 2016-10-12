package edu.colorado.plv.tracerunner_runtime_instrumentation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class LogDat implements Runnable{
    private final String data;

    LogDat(String data){
        this.data = data;
    }

    @Override
    public void run() {
        if(TraceRunnerRuntimeInstrumentation.printWriter == null){
            TraceRunnerRuntimeInstrumentation.setupNetwork();
        }
        TraceRunnerRuntimeInstrumentation.printWriter.println(data);
    }

}
