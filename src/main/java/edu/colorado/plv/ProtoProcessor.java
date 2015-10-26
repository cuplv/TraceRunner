package edu.colorado.plv;

import com.sun.jdi.*;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by s on 10/13/15.
 * Processor that writes a protobuffer to a file
 */
public class ProtoProcessor implements EventProcessor {
    FileOutputStream output;
    BlockingQueue<CallbackOuterClass.EventInCallback.Builder> toWrite = new LinkedBlockingQueue<>();
    Thread writerThread;

    ProtoProcessor(FileOutputStream output){
        this.output = output;
        writerThread = new Thread(new FileWriter(output, toWrite));
        writerThread.start();
        //eventsInCurrentCallback = new ArrayList<>();
    }
    Map<String, Value> currentMessage = null;
    //List<CallbackOuterClass.EventInCallback.Builder> eventsInCurrentCallback;





    public static List<String> getfields(){
        List<String> out = new ArrayList<>();
//        out.add("android.os.MessageZ.what");
//        out.add("android.os.Message.when");
//        out.add("android.os.Message.target");
//        out.add("android.os.Message.callback");
        out.add("what");
        out.add("Message.when");
        out.add("target");
        out.add("callback");
        return out;
    }

    public static void readInputStream(InputStream fileInputStream) throws IOException {
        while(true) {
            CallbackOuterClass.EventInCallback callback =
                    CallbackOuterClass.EventInCallback.parseDelimitedFrom(fileInputStream);
            if(callback == null){
                return;
            }
            System.out.println(callback);
            System.out.println("-----------------------------------------------------------");
        }
    }

    private class FileWriter implements Runnable{
        private final FileOutputStream outfile;
        private final BlockingQueue<CallbackOuterClass.EventInCallback.Builder> toWrite;

        FileWriter(FileOutputStream outfile, BlockingQueue<CallbackOuterClass.EventInCallback.Builder> toWrite){

            this.toWrite = toWrite;
            this.outfile = outfile;
        }

        @Override
        public void run() {
            for(;;) {
                CallbackOuterClass.EventInCallback callback = null;
                try {
                    callback = toWrite.take().build();
                    //System.out.println("wrote record, " + toWrite.size() + " to go");
                } catch (InterruptedException e) {
                    return;
                }
                try {
                    callback.writeDelimitedTo(outfile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    @Override
    public void processMessage(BreakpointEvent brEvt) throws IncompatibleThreadStateException, AbsentInformationException {
        ThreadReference threadRef = brEvt.thread();
        StackFrame stackFrame = threadRef.frame(0);
        List visVars = stackFrame.visibleVariables();
        LocalVariable msgvar = (LocalVariable) visVars.get(0); //Should only be one
        Value value = stackFrame.getValue(msgvar);
        List<Field> msgFields = null;
        Map<String, Value> retrievedValues = new HashMap<>();

        if (value instanceof ObjectReference) {
            ObjectReference ovalue = (ObjectReference) value;
            msgFields = ovalue.referenceType().allFields();
            List<String> recordedFields = getfields();
            for (String sf : recordedFields) {
                for (Field f : msgFields) {
                    if (f.name().equals(sf)) {
                        //Get value and add to return obj
                        Value v = ovalue.getValue(f);
                        retrievedValues.put(sf, v);

                    }
                }
            }
        }
        //Do something with data:
        System.out.println(retrievedValues);
        CallbackOuterClass.Callback.Builder builder = CallbackOuterClass.Callback.newBuilder();
        builder.setWhat(((IntegerValue) retrievedValues.get("what")).value());
        builder.setWhen(valueToProtobuf(retrievedValues.get("Message.when"), false));
        builder.setTarget(valueToProtobuf(retrievedValues.get("target"), false));
        builder.setCallback(valueToProtobuf(retrievedValues.get("callback"), false));

        CallbackOuterClass.EventInCallback.Builder evt =
                CallbackOuterClass.EventInCallback.newBuilder().setCallback(builder);
        toWrite.add(evt);

    }

//    private void finalizeLastMessage() {
//        if (currentMessage != null) {
//
//            CallbackOuterClass.Callback.Builder builder = CallbackOuterClass.Callback.newBuilder();
//            builder.setWhat(((IntegerValue) currentMessage.get("what")).value());
//            builder.setWhen(valueToProtobuf(currentMessage.get("Message.when"), false));
//            builder.setTarget(valueToProtobuf(currentMessage.get("target"), false));
//            builder.setCallback(valueToProtobuf(currentMessage.get("callback"), false));
//            //TODO: serialize messages
//            try {
//                toWrite.put(builder.build());
//            } catch (InterruptedException e) {
//                //should never happen
//            }
//            //TODO: add to  queue
//            eventsInCurrentCallback.clear();
//
//
//
//        }
//
//    }

    @Override
    public void processInvoke(MethodEntryEvent evt, boolean isCallback) {

        Method method = evt.method();
        System.out.println(method);
        boolean isStatic = evt.method().isStatic();
        ThreadReference threadRef = evt.thread();
        long threadID = threadRef.uniqueID();
        String signature = method.signature();
        ReferenceType declaringType = method.declaringType();
        List<StackFrame> stackFrames;
        CallbackOuterClass.PValue caller = null;
        CallbackOuterClass.PValue calle = null;
        List<CallbackOuterClass.PValue> arguments = new ArrayList<>();
        try {
            stackFrames = threadRef.frames();
            int level = 0;
            for(StackFrame stackFrame : stackFrames){
                ObjectReference objectReference = stackFrame.thisObject();

                if (level == 0) {
                    calle = valueToProtobuf(objectReference, false);
                    List<Value> argumentsVals = stackFrame.getArgumentValues();
                    for(Value value : argumentsVals){
                        arguments.add(valueToProtobuf(value, false));
                    }

                }
                if (level == 1) {
                    caller = valueToProtobuf(objectReference, false);
                }
                level++;
                if (level > 1) {
                    break;
                }
            }
        } catch (IncompatibleThreadStateException e) {
            e.printStackTrace();
        }
        CallbackOuterClass.EventInCallback.Builder nevt = CallbackOuterClass.EventInCallback.newBuilder().setMethodEvent(
                CallbackOuterClass.MethodEvent.newBuilder()
                        .setFullname(evt.method().name())
                        .setIsStatic(isStatic)
                        .setThreadID(threadID)
                        .setSignature(signature)
                        .setCalle(calle)
                        .setCaller(caller)
                        .setDeclaringType(declaringType.toString())
                        .addAllParameters(arguments)
                        .setIsCallback(isCallback)
                        .setEventType(CallbackOuterClass.EventType.METHODENTRY));
        toWrite.add(nevt);
    }

    @Override
    public void done() {
        while(toWrite.size() > 0){
            System.out.println("waiting for write to finish");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        writerThread.interrupt();
    }

    @Override
    public void processMethodExit(MethodExitEvent evt, boolean isCallback) {
        Method method = evt.method();
        String methodname = (method.toString());
        boolean isStatic = evt.method().isStatic();
        ThreadReference threadRef = evt.thread();
        long threadID = threadRef.uniqueID();
        String signature = method.signature();
        ReferenceType declaringType = method.declaringType();
        List<StackFrame> stackFrames;
        CallbackOuterClass.PValue caller = null;
        CallbackOuterClass.PValue calle = null;
        List<CallbackOuterClass.PValue> arguments = new ArrayList<>();
        try {
            stackFrames = threadRef.frames();
            int level = 0;
            for(StackFrame stackFrame : stackFrames){
                ObjectReference objectReference = stackFrame.thisObject();

                if (level == 0) {
                    calle = valueToProtobuf(objectReference, false);
                    List<Value> argumentsVals = stackFrame.getArgumentValues();
                    for(Value value : argumentsVals){
                        arguments.add(valueToProtobuf(value, false));
                    }

                }
                if (level == 1) {
                    caller = valueToProtobuf(objectReference, false);
                }
                level++;
                if (level > 1) {
                    break;
                }
            }
        } catch (IncompatibleThreadStateException e) {
            e.printStackTrace();
        }
        CallbackOuterClass.EventInCallback.Builder nevt = CallbackOuterClass.EventInCallback.newBuilder().setMethodEvent(
                CallbackOuterClass.MethodEvent.newBuilder()
                        .setFullname(evt.method().name())
                        .setIsStatic(isStatic)
                        .setThreadID(threadID)
                        .setSignature(signature)
                        .setCalle(calle)
                        .setCaller(caller)
                        .setDeclaringType(declaringType.toString())
                        .addAllParameters(arguments)
                        .setIsCallback(isCallback)
                        .setEventType(CallbackOuterClass.EventType.METHODEXIT)

        );
        toWrite.add(nevt);
    }

    @Override
    public void processException(ExceptionEvent evt) {
        Location location = evt.location();
        Method method = location.method();

        int lineNumber = location.lineNumber();
        String sourceName;
        try {
            sourceName = location.sourceName();
        } catch (AbsentInformationException e) {
            sourceName = "<<None>>";
        }
        CallbackOuterClass.PValue exception = valueToProtobuf(evt.exception(), true);
        CallbackOuterClass.ExceptionEvent exceptionEvent = CallbackOuterClass.ExceptionEvent.newBuilder()
                .setMethod(CallbackOuterClass.PMethod.newBuilder()
                        .setName(method.name())
                        .setClass_(method.declaringType().name())
                        .build())
                .setLineNumber(lineNumber)
                .setSourceName(sourceName)
                .setException(exception)
                .build();
        CallbackOuterClass.EventInCallback.Builder event =
                CallbackOuterClass.EventInCallback.newBuilder().setExceptionEvent(exceptionEvent);
        toWrite.add(event);
    }

    private static List<Method> getMethods(VirtualMachine vm, String clazz){
        List<ReferenceType> refTypes = vm.allClasses();
        ReferenceType rClazz = null;
        for(ReferenceType refType: refTypes) {
            if(refType.name().equals(clazz)){
                if(rClazz != null){
                    throw new IllegalStateException("More than one class found matching: " + clazz);
                }else{
                    rClazz = refType;
                }
            }
        }
        return rClazz.allMethods();
    }


    public static CallbackOuterClass.PValue valueToProtobuf(Value value, boolean logPrims){
        if(value instanceof IntegerValue) {
            IntegerValue integerValue = (IntegerValue) value;
            return CallbackOuterClass.PValue.newBuilder().setPIntegerValue(integerValue.value()).build();
        }else if(value instanceof StringReference){
            StringReference stringReference = (StringReference) value;
            long id = stringReference.uniqueID();
            String sValue = stringReference.value();
            return CallbackOuterClass.PValue.newBuilder()
                    .setPStringReference(CallbackOuterClass.PStringReference.newBuilder()
                            .setValue(sValue)
                            .setId(id)
                            .build())
                    .build();
        } else if(value instanceof ObjectReference){
            ObjectReference objectReference = (ObjectReference)value;
            long id = objectReference.uniqueID();
            String type = objectReference.type().toString();
            ReferenceType referenceType = objectReference.referenceType();

            List<CallbackOuterClass.PField> primitiveFields = null;
            if(logPrims) {
                primitiveFields = new ArrayList<>();
                if (referenceType instanceof ClassType) {
                    ClassType classType = (ClassType) referenceType;
                    List<Field> fields = classType.fields();
                    for (Field field : fields) {
                        Type fieldType;
                        try {
                            fieldType = field.type();
                        } catch (ClassNotLoadedException e) {
                            fieldType = null;
                        }
                        if (fieldType != null && fieldType instanceof PrimitiveType) {
                            if (fieldType instanceof PrimitiveType) {

                                primitiveFields.add(CallbackOuterClass.PField.newBuilder()
                                        .setFieldValue((valueToProtobuf(objectReference.getValue(field), false)))
                                        .setName(field.name()).build());
                            }
                            if (fieldType instanceof ClassType) {
                                if (fieldType.signature().equals("Ljava/lang/String;")) {
                                    primitiveFields.add(CallbackOuterClass.PField.newBuilder()
                                            .setFieldValue(valueToProtobuf(objectReference.getValue(field), false))
                                            .setName(field.name()).build());
                                }
                            }
                        }
                    }
                }
            }
            if(primitiveFields != null) {
                return CallbackOuterClass.PValue.newBuilder()
                        .setPObjctReferenc(
                                CallbackOuterClass.PObjectReference.newBuilder()
                                        .setId(id)
                                        .setType(type)
                                        .addAllPrimitiveFields(primitiveFields)
                                        .build())
                        .build();
            }else {
                return CallbackOuterClass.PValue.newBuilder()
                        .setPObjctReferenc(
                                CallbackOuterClass.PObjectReference.newBuilder()
                                        .setId(id)
                                        .setType(type)
                                        .build())
                        .build();
            }
        } else if(value instanceof BooleanValue) {
            BooleanValue booleanValue = (BooleanValue) value;
            return CallbackOuterClass.PValue.newBuilder().setPBoolValue(booleanValue.value()).build();
        }else if(value == null){
            //Apparently value is set to null for runtime null values
            return CallbackOuterClass.PValue.newBuilder()
                    .setPNull(true).build();
        }else{
            String tostring = value.toString();
            return CallbackOuterClass.PValue.newBuilder()
                    .setPOtherValue(tostring)
                    .build();
        }
    }
}
