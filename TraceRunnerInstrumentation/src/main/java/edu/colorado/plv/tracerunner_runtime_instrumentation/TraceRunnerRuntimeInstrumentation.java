package edu.colorado.plv.tracerunner_runtime_instrumentation;

import edu.colorado.plv.tracerunner.Tracemsg.TraceMsgContainer;
import edu.colorado.plv.tracerunner.Tracemsg.TraceMsgContainer.CallinMsg;
import edu.colorado.plv.tracerunner.Tracemsg.TraceMsgContainer.ValueMsg;
import edu.colorado.plv.tracerunner.Tracemsg.TraceMsgContainer.TraceMsg;

import java.io.BufferedOutputStream;
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
    static BufferedOutputStream outStream = null;
    static AtomicInteger count = new AtomicInteger(0);



    static ExecutorService executorService = Executors.newFixedThreadPool(1);

    public static void logCallin(String signature, String methodName,
                                 Object[] arguments, Object caller) {
        //called on Activity Thread
        int id = count.getAndIncrement();
        long threadID = Thread.currentThread().getId();

        CallinMsg.Builder callinMsgBuilder = CallinMsg.newBuilder();
        callinMsgBuilder.setSignature(signature);
        callinMsgBuilder.setMethodName(methodName);

        for (Object arg : arguments) {
            callinMsgBuilder.addParamList(getValueMsg(arg));
        }

        if (null != caller) callinMsgBuilder.setCaller(getValueMsg(caller));

        TraceMsg msg = TraceMsg.newBuilder()
                .setType(TraceMsg.MsgType.CALLIN)
                .setMessageId(id)
                .setThreadId(threadID)
                .setCallin(callinMsgBuilder.build()).build();

        TraceMsgContainer container = TraceMsgContainer.newBuilder()
                .setMsg(msg).build();

        executorService.execute(new LogDat(container));
    }

    public static void logCallback(String signature, String methodName, Object[] arguments){

    }

    public static void setupNetwork(){
        try {
            socket = new Socket(hostName, portNumber);
            outStream = new BufferedOutputStream(socket.getOutputStream());
        }catch(IOException e){
            System.out.println("Ciao");
            throw new RuntimeException("TraceRunnerInstrumentation failed to open socket");
        }
    }

    /**
     * Builds the ValueMsg for the object obj
     *
     * @param obj the object to serialize
     * @return a ValueMsg serialization of obj
     */
    private static ValueMsg getValueMsg(Object obj) {
        ValueMsg.Builder valueBuilder = ValueMsg.newBuilder();

        valueBuilder = valueBuilder.setType(obj.getClass().getCanonicalName());
        valueBuilder = valueBuilder.setObjectId(Integer.toHexString(System.identityHashCode(obj)));
        valueBuilder = setValue(valueBuilder, obj);

        return valueBuilder.build();
    }

    /**
     * Extract the value from an object obj
     *
     * Now the function set the value if the type is primitive and
     * fills it with the string conversion (toString).
     *
     * It may *NOT* be the right choice
     *
     * @param builder a ValueMsg builder
     * @param obj the object to get the value from
     * @return the eventually modified builder
     */
    private static ValueMsg.Builder setValue(ValueMsg.Builder builder, Object obj) {
        /* set the value only for numbers and strings */
        if (obj instanceof Boolean ||
                obj instanceof Number ||
                obj instanceof Character ||
                obj instanceof String) {
            builder.setValue(obj.toString());
        }

        return builder;
    }
}

