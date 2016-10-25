package edu.colorado.plv.tracerunner_runtime_instrumentation;

import edu.colorado.plv.tracerunner_runtime_instrumentation.Tracemsg.TraceMsgContainer;
import edu.colorado.plv.tracerunner_runtime_instrumentation.Tracemsg.TraceMsgContainer.CallinEntryMsg;
import edu.colorado.plv.tracerunner_runtime_instrumentation.Tracemsg.TraceMsgContainer.TraceMsg;
import edu.colorado.plv.tracerunner_runtime_instrumentation.Tracemsg.TraceMsgContainer.ValueMsg;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by s on 10/3/16.
 */
public class TraceRunnerRuntimeInstrumentation {
    //    static String hostName = "localhost";
    static int portNumber = 5050;
    static String hostName = "localhost";
    static Socket socket = null;
    static BufferedOutputStream outStream = null;
    static AtomicInteger count = new AtomicInteger(0);

    static ExecutorService executorService = Executors.newFixedThreadPool(1);
    static final long EXECUTOR_TO = 5;

    public static void logCallbackReturn(String signature, String methodName, Object returnVal){

    }

    public static void logCallbackEntry(String signature, String methodName, Object[] arguments){

    }

    public static void logCallinExit(String signature, String methodName, Object returnValue, String location){
        int id = count.getAndIncrement();
        long threadID = Thread.currentThread().getId();

        TraceMsgContainer.CallinExitMsg.Builder callinExitMsgBuilder
                = TraceMsgContainer.CallinExitMsg.newBuilder();
        callinExitMsgBuilder.setSignature(signature);
        callinExitMsgBuilder.setMethodName(methodName);
        callinExitMsgBuilder.setReturnValue(getValueMsg(returnValue));

        TraceMsg msg = TraceMsg.newBuilder()
                .setType(TraceMsg.MsgType.CALLIN_EXIT)
                .setMessageId(id)
                .setThreadId(threadID)
                .setCallinExit(callinExitMsgBuilder)
                .build();
        TraceMsgContainer container = TraceMsgContainer.newBuilder()
                .setMsg(msg).build();
        executorService.execute(new LogDat(container));

    }
    public static void logCallin(String signature, String methodName, //TODO: add location
                                 Object[] arguments, Object caller) {
        //called on Activity Thread
        //TODO: add callers
        int id = count.getAndIncrement();
        long threadID = Thread.currentThread().getId();

        CallinEntryMsg.Builder callinMsgBuilder = CallinEntryMsg.newBuilder();
        callinMsgBuilder.setSignature(signature);
        callinMsgBuilder.setMethodName(methodName);

        for (Object arg : arguments) {
            callinMsgBuilder.addParamList(getValueMsg(arg));
        }

        //if (null != caller) callinMsgBuilder.setCaller(getValueMsg(caller));

        TraceMsg msg = TraceMsg.newBuilder()
                .setType(TraceMsg.MsgType.CALLIN_ENTRY)
                .setMessageId(id)
                .setThreadId(threadID)
                .setCallinEntry(callinMsgBuilder.build()).build();

        TraceMsgContainer container = TraceMsgContainer.newBuilder()
                .setMsg(msg).build();

        executorService.execute(new LogDat(container));
    }

    /**
     * Shutdown the running thread on the pool.
     *
     * Taken from the javadoc of ExecutorService
     *
     */
    static void  shutdownAndAwaitTermination() {
        // Disable new tasks from being submitted
        TraceRunnerRuntimeInstrumentation.executorService.shutdown();

        try {
            // Wait a while for existing tasks to terminate
            if (! TraceRunnerRuntimeInstrumentation.executorService.awaitTermination(
                    TraceRunnerRuntimeInstrumentation.EXECUTOR_TO, TimeUnit.SECONDS)) {
                // cancel currently executing tasks
                TraceRunnerRuntimeInstrumentation.executorService.shutdownNow();
                // Wait a while for tasks to respond to being cancelled
                if (! TraceRunnerRuntimeInstrumentation.executorService.awaitTermination(
                        TraceRunnerRuntimeInstrumentation.EXECUTOR_TO, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            TraceRunnerRuntimeInstrumentation.executorService.shutdownNow();
            // Don't think we have to raise a runtime exception here
            System.err.println("Pool did not terminate");
        }
    }

    public static void logCallback(String signature, String methodName, Object[] arguments){

    }

    public static void setupNetwork() throws IOException {
        socket = new Socket(hostName, portNumber);
        outStream = new BufferedOutputStream(socket.getOutputStream());
    }

    /**
     * Builds the ValueMsg for the object obj
     *
     * @param obj the object to serialize
     * @return a ValueMsg serialization of obj
     */
    private static ValueMsg getValueMsg(Object obj) {
        ValueMsg.Builder valueBuilder = ValueMsg.newBuilder();
        if(obj != null) {
            valueBuilder.toString();

            Class aClass = obj.getClass();
            //name not canonical name used here because canonical name can be null if inner class
            //https://stackoverflow.com/questions/15903672/whats-the-difference-between-name-and-canonicalname
            String name = aClass.getName();
            valueBuilder = valueBuilder.setType(name);
            valueBuilder = valueBuilder.setObjectId(Integer.toHexString(System.identityHashCode(obj)));
            valueBuilder = setValue(valueBuilder, obj);

        }else{
            valueBuilder = valueBuilder.setIsNull(true);
        }
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

