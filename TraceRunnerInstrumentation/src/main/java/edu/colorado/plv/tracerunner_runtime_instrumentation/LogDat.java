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

    public void sendData() {
        if (TraceRunnerRuntimeInstrumentation.socket == null ||
                TraceRunnerRuntimeInstrumentation.socket.isClosed() ||
                ! TraceRunnerRuntimeInstrumentation.socket.isConnected()) {
            TraceRunnerRuntimeInstrumentation.setupNetwork();
            System.out.println("Back from setup");
        }

        try {
            System.out.println("created socket");
            data.writeTo(TraceRunnerRuntimeInstrumentation.socket.getOutputStream());
            TraceRunnerRuntimeInstrumentation.socket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("TraceRunnerInstrumentation failed to send data!");
        }
    }

    @Override
    public void run() {
        sendData();
    }

}
