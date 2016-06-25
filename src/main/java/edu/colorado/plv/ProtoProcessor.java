package edu.colorado.plv;

import com.sun.jdi.*;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

/**
 * Created by s on 10/13/15.
 * Processor that writes a protobuffer to a file
 */
public class ProtoProcessor implements EventProcessor {
    private static Pattern appPackageRegex = null;

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


    private static void addIfNotIn(String s, JSONArray jsonObject) {
        boolean inArray = false;
        for(Object co : jsonObject){

            if(co.equals(s)){
                inArray = true;
            }
        }
        if(!inArray){
            jsonObject.add(s);
        }
    }
    public static void writeJsonTraceForVerif(InputStream fileInputStream, OutputStream outputStream, String info) throws IOException{

        JSONArray events = new JSONArray();
        JSONObject event = new JSONObject();
        JSONArray callbackList = new JSONArray();
        long activityThread = -1;

        event.put("initial", "true");
        JSONObject currentCallback = new JSONObject();
        currentCallback.put("callinList", new JSONArray());
        while(true){

            CallbackOuterClass.EventInCallback pEvent =
                    CallbackOuterClass.EventInCallback.parseDelimitedFrom(fileInputStream);
            if(pEvent == null){
                break;
            }

            if(pEvent.getEventTypeCase().equals(CallbackOuterClass.EventInCallback.EventTypeCase.CALLBACK)) {
                //This is an "Event", callback was used but bad terminology

                event.put("callbackList", callbackList);
                events.add(event);
                event = new JSONObject();
                callbackList = new JSONArray();

                JSONObject eventStr;
                //TODO: create string for event
                eventStr = new JSONObject();
                CallbackOuterClass.Callback icallback = pEvent.getCallback();
                eventStr.put("what", icallback.getWhat());
                eventStr.put("callbackField", icallback.getCallback().getPObjctReferenc().getType());
                eventStr.put("targetField", icallback.getTarget().getPObjctReferenc().getType());

                event.put("signature", eventStr.toJSONString());
                event.put("initial", "false");
                System.out.println("****************EVENT*********************");
                System.out.println(event);
                System.out.println("*************************************");

                //Add list of all objects which have been encountered
                JSONArray callbackObjects = new JSONArray();
                event.put("callbackObjects", callbackObjects);

                //Add list of callins
//                JSONArray callinbackList = new JSONArray();
//                event.put("callbackList", callinbackList);


            }else if(pEvent.getEventTypeCase().equals(CallbackOuterClass.EventInCallback.EventTypeCase.METHODEVENT)) {
                CallbackOuterClass.MethodEvent methodEvent = pEvent.getMethodEvent();
                if(methodEvent.getIsCallback()){
                    if(methodEvent.getEventType().equals(CallbackOuterClass.EventType.METHODENTRY)) {
                        //Callback, adding all concrete objects to a list
                        CallbackOuterClass.PValue reciever = methodEvent.getCalle();

                        String s = ToString.to_str(reciever);
                        JSONArray callbackObjects = (JSONArray) event.get("callbackObjects");
                        addIfNotIn(s, callbackObjects);

                        //Iterate over all parameters and add to list
                        int parametersCount = methodEvent.getParametersCount();
                        for (int i = 0; i < parametersCount; ++i) {
                            CallbackOuterClass.PValue parameter = methodEvent.getParameters(i);
                            String sp = ToString.to_str(parameter);
                            addIfNotIn(sp, callbackObjects);
                        }
//                    JSONArray icallbackList = (JSONArray)event.get("callbackList");




                        if(methodEvent.getThreadID() == activityThread){
                            currentCallback = new JSONObject();
                            JSONObject callbackInfo = DataProjection.methodEventToJson_short(methodEvent);
                            currentCallback.put("callback", callbackInfo);
                            currentCallback.put("callinList", new JSONArray());
                            callbackList.add(currentCallback);
                        }

                        if(activityThread == -1){
                            if(methodEvent.getFullname().contains("onCreate")){
                                activityThread = methodEvent.getThreadID();
                                currentCallback = new JSONObject();
                                JSONObject callbackInfo = DataProjection.methodEventToJson_short(methodEvent);
                                currentCallback.put("callback", callbackInfo);
                                currentCallback.put("callinList", new JSONArray());
                                callbackList.add(currentCallback);
                            }
                        }


                        System.out.println("****************Callback*********************");
                        System.out.println(currentCallback);
                        System.out.println("*************************************");
                    }

                }else{
                    //Callin

                    if(methodEvent.getEventType().equals(CallbackOuterClass.EventType.METHODENTRY)) {
                        JSONObject jsonObject = DataProjection.methodEventToJson_short(methodEvent);
                        JSONArray callinList = (JSONArray) currentCallback.get("callinList");
                        if(methodEvent.getThreadID() == activityThread){
                            callinList.add(jsonObject);
                        }

                        if(activityThread == -1){
                            if(methodEvent.getFullname().contains("onCreate")){
                                activityThread = methodEvent.getThreadID();
                                callinList.add(jsonObject);
                            }
                        }
                    }


                }

            }


        }
        events.add(event);
        JSONObject obj = new JSONObject();
        obj.put("info", info);
        obj.put("events", events);
        PrintStream printStream = new PrintStream(outputStream);
        printStream.print(obj.toJSONString());
    }


    public static List<String> getfields(){
        List<String> out = new ArrayList<>();
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
    public void setAppPackageRegex(Pattern appPackageRegex) {
        this.appPackageRegex = appPackageRegex;
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
        builder.setThreadID(threadRef.uniqueID());

        CallbackOuterClass.EventInCallback.Builder evt =
                CallbackOuterClass.EventInCallback.newBuilder().setCallback(builder);
        toWrite.add(evt);

    }

    @Override
    public void processErrorLog(BreakpointEvent evt, String type) throws IncompatibleThreadStateException, AbsentInformationException {
        ThreadReference threadRef = evt.thread();
        StackFrame stackFrame = threadRef.frame(0);
        List<LocalVariable> visVars = stackFrame.visibleVariables();
        CallbackOuterClass.PValue tag = null;
        CallbackOuterClass.PValue msg = null;
        for(LocalVariable l : visVars){
            if(l.name().equals("tag")){
                Value tagv = stackFrame.getValue(l);
                tag = valueToProtobuf(tagv,false);
            }else if(l.name().equals("msg")){
                Value msgv = stackFrame.getValue(l);
                msg = valueToProtobuf(msgv, false);
            }

        }
        CallbackOuterClass.Loge.Builder builder = CallbackOuterClass.Loge.newBuilder();
        if(msg != null)
            builder.setMsg(msg);
        if(tag != null)
            builder.setTag(tag);

        builder.setThreadID(threadRef.uniqueID());
        if(type.equals("e"))
            builder.setLogType(CallbackOuterClass.Loge.LogType.valueOf(CallbackOuterClass.Loge.LogType.LOGE_VALUE));
        else if(type.equals("w"))
            builder.setLogType(CallbackOuterClass.Loge.LogType.valueOf(CallbackOuterClass.Loge.LogType.LOGW_VALUE));
        else if(type.equals("wtf"))
            builder.setLogType(CallbackOuterClass.Loge.LogType.valueOf(CallbackOuterClass.Loge.LogType.LOGWTF_VALUE));
        CallbackOuterClass.EventInCallback.Builder ebuilder =
                CallbackOuterClass.EventInCallback.newBuilder().setLoge(builder);
        toWrite.add(ebuilder);
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
    public void processInvoke(MethodEntryEvent evt, boolean isCallback, boolean isCallIn) {

        Method method = evt.method();
        Location location = method.location();
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
            StackFrame stackFrame = threadRef.frame(0);
            ObjectReference objectReference = stackFrame.thisObject();
            calle = valueToProtobuf(objectReference, false);
            List<Value> argumentsVals = stackFrame.getArgumentValues();
            for(Value value : argumentsVals){
                arguments.add(valueToProtobuf(value,false));
            }
            if(threadRef.frameCount()>1) {
                stackFrame = threadRef.frame(1);
                caller = valueToProtobuf(stackFrame.thisObject(),false);
            }
        } catch (IncompatibleThreadStateException e) {
            e.printStackTrace();
        }
        String methodLocation = location.method().toString();
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
                        .setIsCallIn(isCallIn)
                        .setEventType(CallbackOuterClass.EventType.METHODENTRY)
                        .setMethodLocation(methodLocation)
        );

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
        Location location = method.location();
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
            StackFrame stackFrame = threadRef.frame(0);
            ObjectReference objectReference = stackFrame.thisObject();
            calle = valueToProtobuf(objectReference, false);
            List<Value> argumentsVals = stackFrame.getArgumentValues();
            for(Value value : argumentsVals){
                arguments.add(valueToProtobuf(value,false));
            }
            if(threadRef.frameCount()>1) {
                stackFrame = threadRef.frame(1);
                caller = valueToProtobuf(stackFrame.thisObject(),false);
            }
        } catch (IncompatibleThreadStateException e) {
            e.printStackTrace();
        }
        String methodLocation = location.method().toString();
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
                        .setMethodLocation(methodLocation)

        );
        toWrite.add(nevt);
    }

    @Override
    public void processException(ExceptionCache exceptionCache) {
        long altUniqueId = exceptionCache.getUniqueID();
        if(exceptionCache.isExceptionInfo()) {

            String name = exceptionCache.getName();
            String declaringType = exceptionCache.getDeclaringType();
            long uniqueID = exceptionCache.getUniqueID();
            CallbackOuterClass.PValue exception = exceptionCache.getException();


            int lineNumber1 = exceptionCache.getLineNumber();
            int lineNumber = lineNumber1;
            String sourceName = exceptionCache.getSourceName();


            CallbackOuterClass.ExceptionEvent exceptionEvent = CallbackOuterClass.ExceptionEvent.newBuilder()
                    .setMethod(CallbackOuterClass.PMethod.newBuilder()
                            .setName(name)
                            .setClass_(declaringType)
                            .build())
                    .setLineNumber(lineNumber)
                    .setSourceName(sourceName)
                    .setException(exception)
                    .setThreadID(uniqueID)
                    .build();
            CallbackOuterClass.EventInCallback.Builder event =
                    CallbackOuterClass.EventInCallback.newBuilder().setExceptionEvent(exceptionEvent);
            toWrite.add(event);
        }else{
            //Empty exception (will happen if exception happens but isn't observed)

            toWrite.add(CallbackOuterClass.EventInCallback.newBuilder()
                    .setExceptionEvent(CallbackOuterClass.ExceptionEvent.newBuilder()
                            .setThreadID(altUniqueId)));
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

    public static ClassType getFirstFrameworkParent(ClassType c){
        ClassType superclass = c.superclass();
        if(superclass == null){
            return null;
        }
        String s = c.toString().split(" ")[1];
        if (appPackageRegex.matcher(s).matches()) {
            return getFirstFrameworkParent(superclass);
        } else {
            return c;
        }
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

            ClassType frameworkParent;
            List<InterfaceType> interfaceTypes;
            if(referenceType instanceof ClassType){
                ClassType referenceType1 = (ClassType) referenceType;
                frameworkParent = getFirstFrameworkParent(referenceType1);
                interfaceTypes = referenceType1.allInterfaces();
            }else{
                frameworkParent = null;
                interfaceTypes = new ArrayList<>();
            }

            List<String> interfaces = new ArrayList<>();
            for (InterfaceType interfaceType : interfaceTypes) {
                interfaces.add(interfaceType.toString());
            }


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
            //TODO: add class hierachy

            String value1 = "";
            if(frameworkParent != null)
                value1 = frameworkParent.toString();

            if(primitiveFields != null) {
                return CallbackOuterClass.PValue.newBuilder()
                        .setPObjctReferenc(
                                CallbackOuterClass.PObjectReference.newBuilder()
                                        .setId(id)
                                        .setType(type)
                                        .addAllPrimitiveFields(primitiveFields)
                                        .setFirstFrameworkSuper(value1)
                                        .addAllInterfaces(interfaces)
                                        .build())
                        .build();
            }else {
                return CallbackOuterClass.PValue.newBuilder()
                        .setPObjctReferenc(
                                CallbackOuterClass.PObjectReference.newBuilder()
                                        .setId(id)
                                        .setType(type)
                                        .setFirstFrameworkSuper(value1)
                                        .addAllInterfaces(interfaces)
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
