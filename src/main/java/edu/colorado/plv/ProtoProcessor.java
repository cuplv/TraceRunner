package edu.colorado.plv;

import com.google.protobuf.ByteString;
import com.sun.jdi.*;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.MethodEntryEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    BlockingQueue<CallbackOuterClass.Callback> toWrite = new LinkedBlockingQueue<>();
    Thread writerThread;

    ProtoProcessor(FileOutputStream output){
        this.output = output;
        writerThread = new Thread(new FileWriter(output, toWrite));
        writerThread.start();
    }
    Map<String, Value> currentMessage = null;





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
    private class FileWriter implements Runnable{
        private final FileOutputStream outfile;
        private final BlockingQueue<CallbackOuterClass.Callback> toWrite;

        FileWriter(FileOutputStream outfile, BlockingQueue<CallbackOuterClass.Callback> toWrite){

            this.toWrite = toWrite;
            this.outfile = outfile;
        }

        @Override
        public void run() {
            for(;;) {
                CallbackOuterClass.Callback callback = null;
                try {
                    callback = toWrite.take();
                } catch (InterruptedException e) {
                    return;
                }
                System.out.println(callback);
                byte[] bytes = callback.toByteArray();
                //Note that if a callback trace exceeds 2G it will probably break
                //I can't imagine this will ever happen
                String size = Integer.toString(bytes.length);
                int padding = 10 - size.length();
                for(int i = 0; i<padding; ++i){
                    size = "0" + size;
                }
                try {
                    outfile.write(size.getBytes());
                    outfile.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }
    }

    @Override
    public void processMessage(BreakpointEvent brEvt) throws IncompatibleThreadStateException, AbsentInformationException {
        ThreadReference threadRef = brEvt.thread();
        StackFrame stackFrame = null;
        stackFrame = threadRef.frame(0);
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
        finalizeLastMessage();
        currentMessage = retrievedValues;
    }

    private void finalizeLastMessage() {
        if (currentMessage != null) {

            CallbackOuterClass.Callback.Builder builder = CallbackOuterClass.Callback.newBuilder();
            builder.setWhat(((IntegerValue)currentMessage.get("what")).value());
            builder.setWhen(valueToProtobuf(currentMessage.get("Message.when")));
            builder.setTarget(valueToProtobuf(currentMessage.get("target")));
            builder.setCallback(valueToProtobuf(currentMessage.get("callback")));
            //TODO: serialize messages
            try {
                toWrite.put(builder.build());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //TODO: add to  queue



        }

    }

    @Override
    public void processInvoke(MethodEntryEvent evt) {

        String method = (evt.method().toString());
        System.out.println(method);
    }

    @Override
    public void done() {
        finalizeLastMessage();
        while(toWrite.size() > 0){
            System.out.println("waiting for write to finish");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            writerThread.interrupt();
        }
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

    public static CallbackOuterClass.PValue valueToProtobuf(Value value){
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
            return CallbackOuterClass.PValue.newBuilder()
                    .setPObjctReferenc(
                            CallbackOuterClass.PObjectReference.newBuilder()
                                    .setId(id)
                                    .setType(type)
                                    .build())
                    .build();
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
                    .setPOtherValueBytes(ByteString.copyFromUtf8(tostring))
                    .build();
        }
    }
}
