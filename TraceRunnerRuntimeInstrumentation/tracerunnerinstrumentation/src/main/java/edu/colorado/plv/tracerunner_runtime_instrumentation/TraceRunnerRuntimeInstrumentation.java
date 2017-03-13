package edu.colorado.plv.tracerunner_runtime_instrumentation;

import edu.colorado.plv.tracerunner_runtime_instrumentation.Tracemsg.TraceMsgContainer;
import edu.colorado.plv.tracerunner_runtime_instrumentation.Tracemsg.TraceMsgContainer.CallinEntryMsg;
import edu.colorado.plv.tracerunner_runtime_instrumentation.Tracemsg.TraceMsgContainer.TraceMsg;
import edu.colorado.plv.tracerunner_runtime_instrumentation.Tracemsg.TraceMsgContainer.ValueMsg;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import android.os.Looper;

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

    public static void logException(Throwable t, String signature, String methodName){
        int id = count.getAndIncrement();
        boolean isActivityThread = Looper.getMainLooper().getThread() == Thread.currentThread();
        String message = t.getMessage();
        StackTraceElement[] stackTrace = t.getStackTrace();

        String className = stackTrace[0].getClassName();
        if(FrameworkResolver.get().isFramework(className)) {

            TraceMsgContainer.CallinExceptionMsg.Builder builder
                    = TraceMsgContainer.CallinExceptionMsg.newBuilder();
            builder.setType(t.getClass().getName());
            builder.setThrowingClassName(signature);
            builder.setThrowingMethodName(methodName);
            if (message != null) {
                builder.setExceptionMessage(message);
            }

            for (StackTraceElement stackTraceElement : stackTrace) {
                String stMethodName = stackTraceElement.getMethodName();
                String stClassName = stackTraceElement.getClassName();
                TraceMsgContainer.StackTrace.Builder protoStackTrace
                        = TraceMsgContainer.StackTrace.newBuilder();
                protoStackTrace.setMethod(stMethodName);
                protoStackTrace.setClassName(stClassName);
                builder.addStackTrace(protoStackTrace);
            }

            TraceMsg msg = TraceMsg.newBuilder()
                    .setType(TraceMsg.MsgType.CALLIN_EXEPION)
                    .setMessageId(id)
                    .setThreadId(Thread.currentThread().getId())
                    .setIsActivityThread(isActivityThread)
                    .setCallinException(builder)
                    .build();
            TraceMsgContainer container = TraceMsgContainer.newBuilder().setMsg(msg).build();
            executorService.execute(new LogDat(container));
        }
    }
    public static void logCallbackException(Throwable t, String signature, String methodName){
        int id = count.getAndIncrement();
        String message = t.getMessage();
        boolean isActivityThread = Looper.getMainLooper().getThread() == Thread.currentThread();
        StackTraceElement[] stackTrace = t.getStackTrace();
        String className = stackTrace[0].getClassName();
        TraceMsgContainer.CallbackExceptionMsg.Builder builder
                = TraceMsgContainer.CallbackExceptionMsg.newBuilder();
        builder.setType(t.getClass().getName());
        builder.setThrowingClassName(signature);
        builder.setThrowingMethodName(methodName);
        if (message != null) {
            builder.setExceptionMessage(message);
        }

        for (StackTraceElement stackTraceElement : stackTrace) {
            String stMethodName = stackTraceElement.getMethodName();
            String stClassName = stackTraceElement.getClassName();
            TraceMsgContainer.StackTrace.Builder protoStackTrace
                    = TraceMsgContainer.StackTrace.newBuilder();
            protoStackTrace.setMethod(stMethodName);
            protoStackTrace.setClassName(stClassName);
            builder.addStackTrace(protoStackTrace);
        }

        TraceMsg msg = TraceMsg.newBuilder()
                .setType(TraceMsg.MsgType.CALLBACK_EXCEPTION)
                .setMessageId(id)
                .setThreadId(Thread.currentThread().getId())
                .setCallbackException(builder)
                .setIsActivityThread(isActivityThread)
                .build();
        TraceMsgContainer container = TraceMsgContainer.newBuilder().setMsg(msg).build();
        executorService.execute(new LogDat(container));
    }
    public static void logCallbackReturn(String signature, String methodName, Object returnVal){
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        boolean isActivityThread = Looper.getMainLooper().getThread() == Thread.currentThread();

        StackTraceElement callbackCaller = stackTrace[4];
        String callerClassName = callbackCaller.getClassName();
        if(FrameworkResolver.get().isFramework(callerClassName)) {
            int id = count.getAndIncrement();
            long threadID = Thread.currentThread().getId();
            TraceMsgContainer.CallbackExitMsg.Builder builder
                    = TraceMsgContainer.CallbackExitMsg.newBuilder();
            builder.setClassName(signature);
            builder.setMethodName(methodName);
            if (returnVal != null) {
                builder.setReturnValue(getValueMsg(returnVal));
            }
            TraceMsg msg = TraceMsg.newBuilder()
                    .setType(TraceMsg.MsgType.CALLBACK_EXIT)
                    .setMessageId(id)
                    .setThreadId(threadID)
                    .setCallbackExit(builder)
                    .setIsActivityThread(isActivityThread)
                    .build();
            TraceMsgContainer container = TraceMsgContainer.newBuilder()
                    .setMsg(msg).build();
            executorService.execute(new LogDat(container));
        }
    }

    public static void logCallbackEntry(String signature, String methodName, String[] argumentTypes, String returnType, Object[] arguments, String simpleMethodName){

        //Get caller info
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        boolean isActivityThread = Looper.getMainLooper().getThread() == Thread.currentThread();

        StackTraceElement callbackCaller = stackTrace[4];
        String callerClassName = callbackCaller.getClassName();
        if(FrameworkResolver.get().isFramework(callerClassName)) {
            //Get class hierarchy info
            Class firstFramework = null;
            List<Method> frameworkOverride = null;

            //TODO: get first framework override
            if (arguments[0] != null) {
                firstFramework = FrameworkResolver.get()
                        .getFirstFrameworkClass(arguments[0].getClass());
                if (methodName.contains("<init>")) {

                } else {
                    try {
                        frameworkOverride = FrameworkResolver.get()
                                .getFrameworkOverrideMemo(arguments[0].getClass(), simpleMethodName, argumentTypes);
                    } catch (ClassNotFoundException e) {
                        //parsing soot signature for method is probably most brittle part so log
                        //problems well
                        throw new RuntimeException("class not found exception for: " + methodName, e);
                    }
                }
            }

            //Get callback info

            String callerMethodName = callbackCaller.getMethodName();
            int id = count.getAndIncrement();
            long threadID = Thread.currentThread().getId();
            TraceMsgContainer.CallbackEntryMsg.Builder callbackEntryMsgBuilder
                    = TraceMsgContainer.CallbackEntryMsg.newBuilder();
            callbackEntryMsgBuilder.setClassName(signature);
            callbackEntryMsgBuilder.setMethodName(methodName);
            callbackEntryMsgBuilder.setCallbackCallerClass(callerClassName);
            callbackEntryMsgBuilder.setCallbackCallerMethod(callerMethodName);
            callbackEntryMsgBuilder.setMethodReturnType(returnType);
            for (String argumentType : argumentTypes) {
                callbackEntryMsgBuilder.addMethodParameterTypes(argumentType);
            }
            if (frameworkOverride != null) {

                for (Method frameworkOverrideItem : frameworkOverride) {
                    TraceMsgContainer.FrameworkOverride.Builder builder
                            = TraceMsgContainer.FrameworkOverride.newBuilder();

                    builder.setIsInterface(frameworkOverrideItem.getDeclaringClass().isInterface());

                    builder.setClassName(frameworkOverrideItem.getDeclaringClass().getName());
                    builder.setMethod(FrameworkResolver.sootSignatureFromJava(frameworkOverrideItem));
                    callbackEntryMsgBuilder.addFrameworkOverrides(builder);

                }
//            callbackEntryMsgBuilder.setFirstFrameworkOverrideClass(
//                    frameworkOverride.getClass().getName());
//            callbackEntryMsgBuilder.setFirstFrameworkOverrideMethod(
//                    FrameworkResolver.sootSignatureFromJava(frameworkOverride));
            }
            if (firstFramework != null) {
                callbackEntryMsgBuilder.setReceiverFirstFrameworkSuper(firstFramework.getName());
//            callbackEntryMsgBuilder.setFirstFrameworkOverrideClass(firstFramework.getName());
            }
            for (Object o : arguments) {
                callbackEntryMsgBuilder.addParamList(getValueMsg(o));
            }

            TraceMsg msg = TraceMsg.newBuilder()
                    .setType(TraceMsg.MsgType.CALLBACK_ENTRY)
                    .setMessageId(id)
                    .setThreadId(threadID)
                    .setCallbackEntry(callbackEntryMsgBuilder)
                    .setIsActivityThread(isActivityThread)
                    .build();
            TraceMsgContainer container = TraceMsgContainer.newBuilder()
                    .setMsg(msg).build();
            executorService.execute(new LogDat(container));
        }
    }

    public static void logCallinExit(String signature, String methodName, Object returnValue){
        if(FrameworkResolver.get().isFramework(signature)) {
            int id = count.getAndIncrement();
            boolean isActivityThread = Looper.getMainLooper().getThread() == Thread.currentThread();

            long threadID = Thread.currentThread().getId();

            TraceMsgContainer.CallinExitMsg.Builder callinExitMsgBuilder
                    = TraceMsgContainer.CallinExitMsg.newBuilder();
            callinExitMsgBuilder.setClassName(signature);
            callinExitMsgBuilder.setMethodName(methodName);

            if(!methodName.matches("void .*"))
                callinExitMsgBuilder.setReturnValue(getValueMsg(returnValue));

            TraceMsg msg = TraceMsg.newBuilder()
                    .setType(TraceMsg.MsgType.CALLIN_EXIT)
                    .setMessageId(id)
                    .setThreadId(threadID)
                    .setCallinExit(callinExitMsgBuilder)
                    .setIsActivityThread(isActivityThread)
                    .build();
            TraceMsgContainer container = TraceMsgContainer.newBuilder()
                    .setMsg(msg).build();
            executorService.execute(new LogDat(container));
        }
    }
    public static void logCallin(String signature, String methodName, //TODO: add location
                                 Object[] arguments, Object caller, String file, String linecolumn) {
        if(FrameworkResolver.get().isFramework(signature)) {
            boolean isActivityThread = Looper.getMainLooper().getThread() == Thread.currentThread();

            //called on Activity Thread
            //TODO: add callers
            int id = count.getAndIncrement();
            long threadID = Thread.currentThread().getId();

            CallinEntryMsg.Builder callinMsgBuilder = CallinEntryMsg.newBuilder();
            callinMsgBuilder.setClassName(signature);
            callinMsgBuilder.setMethodName(methodName);
            callinMsgBuilder.setFile(file);
            callinMsgBuilder.setLineCol(linecolumn);


            for (Object arg : arguments) {
                callinMsgBuilder.addParamList(getValueMsg(arg));
            }

            //if (null != caller) callinMsgBuilder.setCaller(getValueMsg(caller));

            TraceMsg msg = TraceMsg.newBuilder()
                    .setType(TraceMsg.MsgType.CALLIN_ENTRY)
                    .setMessageId(id)
                    .setThreadId(threadID)
                    .setIsActivityThread(isActivityThread)
                    .setCallinEntry(callinMsgBuilder.build()).build();

            TraceMsgContainer container = TraceMsgContainer.newBuilder()
                    .setMsg(msg).build();

            executorService.execute(new LogDat(container));
        }
    }

    /**
     * Shutdown the running thread on the pool.
     *
     * Taken from the javadoc of ExecutorService
     *
     */
    public static void  shutdownAndAwaitTermination() {
//        // Disable new tasks from being submitted
//        TraceRunnerRuntimeInstrumentation.executorService.shutdown();
//
//        try {
//            // Wait a while for existing tasks to terminate
//            if (! TraceRunnerRuntimeInstrumentation.executorService.awaitTermination(
//                    TraceRunnerRuntimeInstrumentation.EXECUTOR_TO, TimeUnit.SECONDS)) {
//                // cancel currently executing tasks
//                TraceRunnerRuntimeInstrumentation.executorService.shutdownNow();
//                // Wait a while for tasks to respond to being cancelled
//                if (! TraceRunnerRuntimeInstrumentation.executorService.awaitTermination(
//                        TraceRunnerRuntimeInstrumentation.EXECUTOR_TO, TimeUnit.SECONDS))
//                    System.err.println("Pool did not terminate");
//            }
//        } catch (InterruptedException ie) {
//            TraceRunnerRuntimeInstrumentation.executorService.shutdownNow();
//            // Don't think we have to raise a runtime exception here
//            System.err.println("Pool did not terminate");
//        }
    }

//    public static void logCallback(String signature, String methodName, Object[] arguments){
//
//    }

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

