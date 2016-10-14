package edu.colorado.plv.tracerunner_runtime_instrumentation;

import edu.colorado.plv.tracerunner.Tracemsg;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class LogDat implements Runnable {
    private final Tracemsg.TraceMsgContainer data;

    LogDat(Tracemsg.TraceMsgContainer data) {
        this.data = data;
    }

    @Override
    public void run() {
        if (TraceRunnerRuntimeInstrumentation.socket == null ||
                TraceRunnerRuntimeInstrumentation.socket.isClosed() ||
                ! TraceRunnerRuntimeInstrumentation.socket.isConnected()) {
            TraceRunnerRuntimeInstrumentation.setupNetwork();
        }

        try {
            data.writeTo(TraceRunnerRuntimeInstrumentation.socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException("TraceRunnerInstrumentation failed to send data!");
        }
    }

}
