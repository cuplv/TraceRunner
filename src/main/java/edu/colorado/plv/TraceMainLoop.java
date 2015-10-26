package edu.colorado.plv;

import com.sun.jdi.*;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by s on 10/13/15.
 */

public class TraceMainLoop {
    private final int port;
    private final String appPackage;

    private EventProcessor eventProcessor;
    private List<String> filters;
    private List<MethodEntryRequest> entryToToggle;
    private List<MethodExitRequest> exitToToggle;
    private void enableAll(){
        for(MethodEntryRequest e: entryToToggle){
            e.enable();
        }
        for(MethodExitRequest e: exitToToggle){
            e.enable();
        }
    }
    private void disableAll(){
        for(MethodEntryRequest e: entryToToggle){
            e.disable();
        }
        for(MethodExitRequest e : exitToToggle){
            e.disable();
        }
    }

    public TraceMainLoop(int port, EventProcessor eventProcessor, List<String> filters, String appPackage) {
        this.port = port;
        this.eventProcessor = eventProcessor;
        this.filters = filters;
        this.appPackage = appPackage;

    }
    public void mainLoop() throws IOException, IllegalConnectorArgumentsException, InterruptedException {



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
        if (socketConnector != null) {
            Map<String, Connector.Argument> paramsMap = socketConnector.defaultArguments();
            Connector.IntegerArgument portArg = (Connector.IntegerArgument) paramsMap.get("port");
            portArg.setValue(port);
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
            List<String> classExclusions = ClassExclusions.getClassExlusions();
            MethodEntryRequest packageEntry = evtReqMgr.createMethodEntryRequest();
            packageEntry.addClassFilter(appPackage);
            packageEntry.enable();
            MethodExitRequest packageExit = evtReqMgr.createMethodExitRequest();
            packageExit.enable();


            for (String filter : filters) {
                MethodEntryRequest methodEntryRequest = evtReqMgr.createMethodEntryRequest();
                methodEntryRequest.addClassFilter(filter);
                MethodExitRequest methodExitRequest = evtReqMgr.createMethodExitRequest();
                methodExitRequest.addClassFilter(filter);

                for(String exclusion: classExclusions){
                    methodEntryRequest.addClassExclusionFilter(exclusion);
                    methodExitRequest.addClassExclusionFilter(exclusion);
                }

                entryToToggle.add(methodEntryRequest);
                exitToToggle.add(methodExitRequest);

//                methodEntryRequest.enable();
//                methodExitRequest.enable();
            }
            ExceptionRequest exceptionRequest;
            exceptionRequest = evtReqMgr.createExceptionRequest(null, true, true);
//            exceptionRequest.addClassExclusionFilter("java.lang.ClassNotFoundException");
            List<String> exceptionExclusions = ClassExclusions.exceptionExclusions();
            for(String exclusion : exceptionExclusions){
                exceptionRequest.addClassExclusionFilter(exclusion);
            }
            exceptionRequest.enable();


            EventQueue evtQueue = vm.eventQueue();


            //Process breakpoints main loop
            Method callback = null; //Null until callback hit

            try {
                while (true) {
                    EventSet evtSet = evtQueue.remove();
                    EventIterator evtIter = evtSet.eventIterator();
                    while (evtIter.hasNext()) {
                        try {
                            Event evt = evtIter.next();
                            if (evt instanceof BreakpointEvent) {
                                eventProcessor.processMessage((BreakpointEvent)evt);
                            }else if (evt instanceof MethodEntryEvent){
                                MethodEntryEvent mevt = (MethodEntryEvent) evt;
                                Method m = mevt.method();
                                String name = m.declaringType().name();
                                eventProcessor.processInvoke(mevt);
                            }else if (evt instanceof MethodExitEvent){
                                eventProcessor.processMethodExit((MethodExitEvent)evt);
                            }else if (evt instanceof ExceptionEvent){
                                eventProcessor.processException((ExceptionEvent)evt);
                            }
//                            Map<String, Value> eventdetails = eventProcessor.processEvent(evt, mentered);
//                            if (eventdetails.size() > 0) {
//                                System.out.println(eventdetails);
//                                for (String method : mentered) {
//                                    System.out.println("     " + method);
//                                }
//                                mentered.clear();
//                            }
                        } catch (Exception exc) {
                            System.out.println(exc.getClass().getName() + ": " + exc.getMessage());
                        } finally {
                            evtSet.resume();
                        }
                    }
                }
                //TODO: https://dzone.com/articles/generating-minable-event
            } catch (VMDisconnectedException e) {
                eventProcessor.done();
                System.out.println("Process exited");

            }
        }
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
    
}
