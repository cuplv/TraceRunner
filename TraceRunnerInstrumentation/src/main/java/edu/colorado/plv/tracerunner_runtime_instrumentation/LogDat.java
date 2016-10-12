package edu.colorado.plv.tracerunner_runtime_instrumentation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class LogDat implements Runnable{
    private final String data;
    private final PrintWriter printwriter;

    LogDat(PrintWriter printwriter, String data){
        this.printwriter = printwriter;
        this.data = data;
    }

    @Override
    public void run() {
        printwriter.write(data);
    }
}
