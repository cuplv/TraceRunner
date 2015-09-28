package edu.tracerunner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.event.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;


/**
 * Created by s on 8/11/15.
 * This is the record everything trace runner, it will probably be too slow and need refactoring.
 * TODO: In progress
 */
public class TraceRunner {
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
    public static void main(String[] args) throws Exception
    {
        int portNum = Integer.parseInt(args[0]);
 //       String clazz = args[1];
        VirtualMachineManager vmMgr = Bootstrap.virtualMachineManager();
        AttachingConnector socketConnector = null;
        List<AttachingConnector> attachingConnectors = vmMgr.attachingConnectors();
        for (AttachingConnector ac: attachingConnectors)
        {
            if (ac.transport().name().equals("dt_socket"))
            {
            socketConnector = ac;
            break;
            }
        }
        if (socketConnector != null)
        {
            Map paramsMap = socketConnector.defaultArguments();
            Connector.IntegerArgument portArg = (Connector.IntegerArgument)paramsMap.get("port");
            portArg.setValue(portNum);
            VirtualMachine vm = socketConnector.attach(paramsMap);
            System.out.println("Attached to process '" + vm.name() + "'");



            Method dispatchMessage = getDispatchMessage(vm);
            //List<Method> targetMethods = getMethods(vm, clazz);



            //Set breakpoint
            Location breakpointLocation = dispatchMessage.location();

            EventRequestManager evtReqMgr = vm.eventRequestManager();
            BreakpointRequest bReq
                    = evtReqMgr.createBreakpointRequest(breakpointLocation);
            bReq.setSuspendPolicy(BreakpointRequest.SUSPEND_ALL);
            bReq.enable();

            //Method entry notification
            MethodEntryRequest methodEntryRequest = evtReqMgr.createMethodEntryRequest();




            EventQueue evtQueue = vm.eventQueue();


            //Process breakpoints main loop
            while(true) {
                EventSet evtSet = evtQueue.remove();
                EventIterator evtIter = evtSet.eventIterator();
                while(evtIter.hasNext()) {
                    try
                    {
                        Event evt = evtIter.next();
                        Map<String, Value> eventdetails = processEvent(evt);
                        System.out.println(eventdetails);
                    }

                    catch (Exception exc)
                    {
                        System.out.println(exc.getClass().getName() + ": " + exc.getMessage());
                    }
                    finally
                    {
                        evtSet.resume();
                    }
                }
            }
            //TODO: https://dzone.com/articles/generating-minable-event

            //TODO: dump message for each hit
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
    private static Method getDispatchMessage(VirtualMachine vm) {
        List<ReferenceType> refTypes = vm.allClasses();
        ReferenceType handlerClass = null;
        for (ReferenceType refType: refTypes) {
            //System.out.println(refType.name());
            if (refType.name().equals("android.os.Handler")) {
                if (handlerClass != null) {
                    throw new IllegalStateException("more than one handler found");
                }else{
                    handlerClass = refType;
                }
            }
        }
        List<Method> methods = handlerClass.allMethods();
        Method dispatchMessage = null;
        for(Method method : methods){
            if(method.name().equals("dispatchMessage")){
                if(method.signature().equals("(Landroid/os/Message;)V")){
                    if(dispatchMessage != null){
                        throw new IllegalStateException("");
                    }
                    dispatchMessage = method;
                }

            }
        }
        return dispatchMessage;
    }


    private static Map<String, Value> processEvent(Event evt) throws IncompatibleThreadStateException, AbsentInformationException {
        Map<String, Value> retrievedValues = new HashMap<>();
        EventRequest evtReq = evt.request();
        if (evtReq instanceof BreakpointRequest) {
            BreakpointEvent brEvt = (BreakpointEvent) evt;
            ThreadReference threadRef = brEvt.thread();
            StackFrame stackFrame = threadRef.frame(0);
            List visVars = stackFrame.visibleVariables();
            LocalVariable msgvar = (LocalVariable) visVars.get(0); //Should only be one
            Value value = stackFrame.getValue(msgvar);
            List<Field> msgFields = null;
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
        }else if(evtReq instanceof MethodEntryRequest) {
            MethodEntryRequest mer = (MethodEntryRequest) evtReq;
            System.out.println("method entry");
        }else {
            System.out.println("Other event occured: " +  evtReq);
        }
        return retrievedValues;
    }
}
