package edu.colorado.plv;

import com.sun.jdi.*;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.MethodEntryEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by s on 10/13/15.
 * Processor that writes a protobuffer to a file
 */
public class ProtoProcessor implements EventProcessor {
    File output;
    ProtoProcessor(File output){
        this.output = output;
    }
    Map<String, Value> currentMessage = null;

    //CallBackOuterClass.CallbackOrBuilder callbackBuilder = new CallbackOuterClass.CallbackOrBuilder();

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
        private final File outfile;

        FileWriter(File outfile){
            //TODO: add BlockingQueue with proto
            this.outfile = outfile;
        }

        @Override
        public void run() {

        }
    }

    @Override
    public void processMessage(BreakpointEvent evt) throws IncompatibleThreadStateException, AbsentInformationException {
        BreakpointEvent brEvt = (BreakpointEvent) evt;
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
}
