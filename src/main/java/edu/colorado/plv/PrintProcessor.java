package edu.colorado.plv;

import com.sun.jdi.*;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;

import java.io.PrintWriter;
import java.util.*;
import java.net.Socket;

/**
 * Created by s on 10/13/15.
 */
public class PrintProcessor implements EventProcessor {

    private int waitTimeForCallbackInSeconds;
    private int cmdPort;
    private String hostname;
    private List<String> filters;
    private volatile List<Boolean> flags = null;
    private Socket socket;
    private PrintWriter printWriter;
    private final Queue<String> traceRunnerMsgs = new LinkedList<>();
    private Timer timer = new Timer();
    private Map<String, List<Value>> entity = new LinkedHashMap<>();

    public PrintProcessor() {}

    public PrintProcessor(int waitTimeForCallbackInSeconds, int cmdPort, String hostname, List<String> filters){
        this.waitTimeForCallbackInSeconds = waitTimeForCallbackInSeconds;
        this.cmdPort = cmdPort;
        this.hostname = hostname;
        this.filters = filters;
        flags = new ArrayList<>();
        flags.add(false);

        setTimerDetails();
    }

    private void setTimerDetails() {

        try{
            socket = new Socket(hostname, cmdPort);
            printWriter = new PrintWriter(socket.getOutputStream(), true);
        }catch(Exception e){
            System.out.println("Error while writing to native script port");
        }
    }


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
        System.out.println("Retrieved values " + retrievedValues);
        processTraceInformation(retrievedValues);
    }

    private void processTraceInformation(Map<String, Value> retrievedValues){

        /*Timer for TraceRunner to stop sending messages*/
        if (!flags.get(0)) {
            flags.set(0, true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {

                    Set<Map.Entry<String, List<Value>>> set = entity.entrySet();
                    Iterator<Map.Entry<String, List<Value>>> it = set.iterator();
                    while(it.hasNext()){
                        Map.Entry<String, List<Value>> element = it.next();
                        StringBuffer buffer = new StringBuffer();
                        buffer.append("Parameter ");
                        buffer.append(element.getKey()+" ");
                        for (Value value : element.getValue()) {
                            if (value != null) {
                                buffer.append(value.toString() + " ");
                            }
                        }
                        traceRunnerMsgs.add(buffer.toString().trim());
                    }
                    //Terminate Sending messages
                    traceRunnerMsgs.add("END");
                    while(!traceRunnerMsgs.isEmpty()) {
                        String msg = traceRunnerMsgs.poll();
                        System.out.println("TraceRunner writes " + msg);
                        printWriter.println(msg);
                    }
                    entity.clear();
                    flags.set(0, false);
                }
            }, waitTimeForCallbackInSeconds * 1000);
        }

        if (retrievedValues != null) {
            if (retrievedValues.size() > 0) {
                Value targetField = retrievedValues.get("target");
                for (String filter : filters) {
                    filter = filter.replace("*", "");
                    if(targetField.toString().contains(filter)){
                        Value whatField = retrievedValues.get("what");
                        traceRunnerMsgs.add(whatField.toString());
                    }
                }
            }
        }
    }

    @Override
    public void processInvoke(MethodEntryEvent evt) {

        if(evt instanceof MethodEntryEvent) {
            MethodEntryEvent mer = (MethodEntryEvent) evt;
            System.out.println("method entry: " + mer.method().toString());
            processTraceInformation(null);//To terminate it after the configured seconds. Need to call it

            Method method = mer.method();
            String methodName = method.name();
            ThreadReference threadRef = evt.thread();
            List<StackFrame> stackFrames;

            try {
                stackFrames = threadRef.frames();
                int level = 0;
                for(StackFrame stackFrame : stackFrames){
                    if (level == 0) {
                        List<Value> argumentsVals = stackFrame.getArgumentValues();
                        if(!entity.containsKey(methodName)){
                            entity.put(methodName, argumentsVals);
                        }
                    }
                    level++;
                    if (level > 1) {
                        break;
                    }
                }
            } catch (IncompatibleThreadStateException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void done() {

    }

    @Override
    public void processMethodExit(MethodExitEvent evt) {

    }

    @Override
    public void processException(ExceptionEvent evt) {

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
