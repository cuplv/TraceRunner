package edu.colorado.plv.tracerunner_runtime_instrumentation;

import edu.colorado.plv.tracerunner_runtime_instrumentation.Tracemsg;

import java.io.IOException;

public class LogDat implements Runnable {
    private final Tracemsg.TraceMsgContainer data;

    LogDat(Tracemsg.TraceMsgContainer data) {
        this.data = data;
    }

    public void sendData() throws IOException {
        if (TraceRunnerRuntimeInstrumentation.socket == null ||
                TraceRunnerRuntimeInstrumentation.socket.isClosed() ||
                !TraceRunnerRuntimeInstrumentation.socket.isConnected()) {
            TraceRunnerRuntimeInstrumentation.setupNetwork();
        }
        data.writeDelimitedTo(TraceRunnerRuntimeInstrumentation.socket.getOutputStream());
        TraceRunnerRuntimeInstrumentation.socket.getOutputStream().flush();
    }

    @Override
    public void run() {
        try {
            sendData();
        } catch (IOException e) {
            throw new RuntimeException("TracerunnerRuntimeInstrumentation: " +
                    "IOException sending data");
        }
    }

}
