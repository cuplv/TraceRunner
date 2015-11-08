package edu.colorado.plv;

import com.sun.jdi.*;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by s on 10/13/15.
 */

public class TraceMainLoop {
    private final int port;
    private final String appPackage;
    private final Pattern appPackageRegex;

    private EventProcessor eventProcessor;
    private List<String> filters;
    private HashMap<ThreadReference,List<MethodEntryRequest>> entryToToggle;
    private HashMap<ThreadReference,List<MethodExitRequest>> exitToToggle;
    private boolean enabledAndroidStar = false;
    EventRequestManager evtReqMgr;


    /**
     * MethodEntryRequests and MethodExitRequests for thread of @param threadReference will be enabled allowing logging
     */
    private void enableAll(ThreadReference threadReference){
        if(entryToToggle.containsKey(threadReference)) {
            List<MethodEntryRequest> entryToTogglel = entryToToggle.get(threadReference);
            for (MethodEntryRequest e : entryToTogglel) {
                e.enable();
            }
        }else {
            List<MethodEntryRequest> mer = new ArrayList<>();
            for (String filter : filters) {
                MethodEntryRequest methodEntryRequest = evtReqMgr.createMethodEntryRequest();
                methodEntryRequest.addClassFilter(filter);

                List<String> classExclusions = Resources.getClassExlusions();

                for(String exclusion: classExclusions){
                    methodEntryRequest.addClassExclusionFilter(exclusion);
                }
                methodEntryRequest.addThreadFilter(threadReference);
                methodEntryRequest.enable();

                mer.add(methodEntryRequest);
            }
            entryToToggle.put(threadReference, mer);
        }
        if(exitToToggle.containsKey(threadReference)) {
            List<MethodExitRequest> exitToTogglel = exitToToggle.get(threadReference);
            for (MethodExitRequest e : exitToTogglel) {
                e.enable();
            }
        }else {
            List<MethodExitRequest> mxr = new ArrayList<>();
            for (String filter : filters) {
                MethodExitRequest methodExitRequest = evtReqMgr.createMethodExitRequest();
                methodExitRequest.addClassFilter(filter);

                List<String> classExclusions = Resources.getClassExlusions();

                for(String exclusion: classExclusions){
                    methodExitRequest.addClassExclusionFilter(exclusion);
                }
                //entryToToggle.add(threadReference, methodEntryRequest);
                methodExitRequest.addThreadFilter(threadReference);
                methodExitRequest.enable();
                mxr.add(methodExitRequest);
            }
            exitToToggle.put(threadReference, mxr);
        }

    }

    /**
     * MethodEntryRequest and MethodExitRequests for thread of @param threadReference will be disabled
     * @param threadReference
     */
    private void disableAll(ThreadReference threadReference){
        if(entryToToggle.containsKey(threadReference)) {
            List<MethodEntryRequest> entryToTogglel = entryToToggle.get(threadReference);
            for (MethodEntryRequest e : entryToTogglel) {
                e.disable();
            }
        }
        if(exitToToggle.containsKey(threadReference)) {
            List<MethodExitRequest> exitToTogglel = exitToToggle.get(threadReference);
            for (MethodExitRequest e : exitToTogglel) {
                e.disable();
            }
        }
    }

    public TraceMainLoop(int port, EventProcessor eventProcessor, List<String> filters, String appPackage) {
        this.port = port;
        this.eventProcessor = eventProcessor;
        this.filters = filters;
        this.appPackage = appPackage;
        this.entryToToggle = new HashMap<>();
        this.exitToToggle = new HashMap<>();
        this.appPackageRegex = GlobUtil.createRegexFromGlob(appPackage);

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


            //Set breakpoint for dispatchMessage
            Location breakpointLocation = dispatchMessage.location();

            evtReqMgr = vm.eventRequestManager();
            BreakpointRequest bReqDispatch
                    = evtReqMgr.createBreakpointRequest(breakpointLocation);
            bReqDispatch.setSuspendPolicy(BreakpointRequest.SUSPEND_ALL);
            bReqDispatch.enable();


            //Set breakpoints for error log
            List<String> sigs = Resources.getLogeSigs();
            Set<BreakpointRequest> logeBp = new HashSet<>();
            for(String sig : sigs) {
                breakpointLocation = getMethodLoc(vm, "android.util.Log", "e", sig).location();
                BreakpointRequest brq = evtReqMgr.createBreakpointRequest(breakpointLocation);
                brq.setSuspendPolicy(BreakpointRequest.SUSPEND_ALL);
                brq.enable();
                logeBp.add(brq);
            }
            Set<BreakpointRequest> logWBreakpoint = new HashSet<>();
            //TODO:
            Set<Method> wlocations = getMethodLocs(vm, "android.util.Log", "w");
            for(Method location : wlocations) {
                breakpointLocation = location.location();
                BreakpointRequest brq = evtReqMgr.createBreakpointRequest(breakpointLocation);
                brq.setSuspendPolicy(BreakpointRequest.SUSPEND_ALL);
                brq.enable();
                logWBreakpoint.add(brq);
            }
            Set<BreakpointRequest> logWTFBreakpoint = new HashSet<>();
            Set<Method> wtfLocations = getMethodLocs(vm, "android.util.Log", "wtf");
            for(Method method: wtfLocations) {
                BreakpointRequest brq = evtReqMgr.createBreakpointRequest(method.location());
                brq.setSuspendPolicy(BreakpointRequest.SUSPEND_ALL);
                brq.enable();
                logWTFBreakpoint.add(brq);
            }

            //Method entry notification

            MethodEntryRequest packageEntry = evtReqMgr.createMethodEntryRequest();
            packageEntry.addClassFilter(appPackage);
            packageEntry.enable();
            MethodExitRequest packageExit = evtReqMgr.createMethodExitRequest();
            packageExit.addClassFilter(appPackage);
            packageExit.enable();



            ExceptionRequest exceptionRequest;
            exceptionRequest = evtReqMgr.createExceptionRequest(null, true, true);
//            exceptionRequest.addClassExclusionFilter("java.lang.ClassNotFoundException");
            List<String> exceptionExclusions = Resources.exceptionExclusions();
            for(String exclusion : exceptionExclusions){
                exceptionRequest.addClassExclusionFilter(exclusion);
            }
            exceptionRequest.enable();


            EventQueue evtQueue = vm.eventQueue();


            //Process breakpoints main loop
            Map<ThreadReference,Method> callback = new HashMap<>(); //Null until callback hit
            Map<ThreadReference,Method> callin = new HashMap<>();
            ThreadReference activityThread = null;
//            Method callIn = null;


            try {
                while (true) {
                    EventSet evtSet = evtQueue.remove();
                    EventIterator evtIter = evtSet.eventIterator();
                    while (evtIter.hasNext()) {
                        try {
                            Event evt = evtIter.next();
                            if (evt instanceof BreakpointEvent) {
                                ThreadReference thrf = ((BreakpointEvent) evt).thread();
                                if(activityThread == null){
                                    activityThread = ((BreakpointEvent) evt).thread();
//                                    packageEntry.addThreadFilter(activityThread);
//                                    packageExit.addThreadFilter(activityThread);
                                }else{
                                    if(!(activityThread.equals(((BreakpointEvent) evt).thread()))){
                                        throw new IllegalStateException("Looper looped from wrong thread");
                                    }
                                    disableAll(((BreakpointEvent) evt).thread());
                                }
                                EventRequest request = evt.request();
                                if(request == bReqDispatch) {
                                    eventProcessor.processMessage((BreakpointEvent) evt);
                                    callback.remove(thrf); //TODO: if additional breakpoints add check for msg here
                                }else if(logeBp.contains(request)){
                                    eventProcessor.processErrorLog((BreakpointEvent)evt, "e");
                                }else if(logWBreakpoint.contains(request)){
                                    eventProcessor.processErrorLog((BreakpointEvent)evt, "w");
                                }else if(logWTFBreakpoint.contains(request)){
                                    eventProcessor.processErrorLog((BreakpointEvent)evt, "wtf");
                                }
                            }else if (evt instanceof MethodEntryEvent){

                                MethodEntryEvent mevt = (MethodEntryEvent) evt;
                                ThreadReference thref = mevt.thread();
                                boolean isCallback = false;
                                Method m = mevt.method();
                                String name = m.declaringType().name();

                                if(!callback.containsKey(thref)) {
                                    //setting callback
                                    if(appPackageRegex.matcher(name).matches()){
                                        //set callback value when app package is first hit
                                        if(!m.name().contains("<init>")) {
                                            if(!callback.containsKey(thref)) {
                                                callback.put(thref,m);
                                                enableAll(thref);

                                            }
                                            isCallback = true;
                                        }
                                    }
                                }else{
                                    //callback hit check for call in
                                    if((!appPackageRegex.matcher(name).matches()) && (!callin.containsKey(thref))){
                                        eventProcessor.processInvoke(mevt, isCallback, true);
                                        callin.put(thref, m);
                                    }

                                }
                                if(!callin.containsKey(thref)) {
                                    eventProcessor.processInvoke(mevt, isCallback, false);
                                }
//                                if(callback != null){
//
//                                }

                            }else if (evt instanceof MethodExitEvent){
                                MethodExitEvent mxe = (MethodExitEvent) evt;
                                ThreadReference thrf = mxe.thread();
                                Method m = mxe.method();
                                boolean isCallback = false;
                                if(callback.containsKey(thrf) && callback.get(thrf).equals(m)){
                                    disableAll(thrf);
                                    callback.remove(thrf);
                                }
                                if(callin.containsKey(thrf) && callin.get(thrf).equals(m)){
                                    callin.remove(thrf);
                                }
//                                if(m == callback){
//                                    callback = null;
//                                    disableAll();
//                                    isCallback = true;
//                                }
                                if(!callin.containsKey(thrf)) {
                                    eventProcessor.processMethodExit(mxe, isCallback);
                                }
                            }else if (evt instanceof ExceptionEvent){
                                ExceptionEvent exn = (ExceptionEvent) evt;
                                exn.location().method();
                                eventProcessor.processException(exn);
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
    private static Set<Method> getMethodLocs(VirtualMachine vm, String clazz, String methodName){
        List<ReferenceType> refTypes = vm.allClasses();
        ReferenceType handlerClass = null;
        for (ReferenceType refType: refTypes) {
            //System.out.println(refType.name());
            if (refType.name().equals(clazz)) {
                if (handlerClass != null) {
                    throw new IllegalStateException("more than one class found: " + clazz);
                }else{
                    handlerClass = refType;
                }
            }
        }
        List<Method> methods = handlerClass.allMethods();
        Set<Method> dispatchMessage = new HashSet<>();
        for(Method method : methods){
            if(method.name().equals(methodName)){
                dispatchMessage.add(method);
            }
        }
        return dispatchMessage;
    }
    private static Method getMethodLoc(VirtualMachine vm, String clazz, String methodName, String sig){
        List<ReferenceType> refTypes = vm.allClasses();
        ReferenceType handlerClass = null;
        for (ReferenceType refType: refTypes) {
            //System.out.println(refType.name());
            if (refType.name().equals(clazz)) {
                if (handlerClass != null) {
                    throw new IllegalStateException("more than one class found: " + clazz);
                }else{
                    handlerClass = refType;
                }
            }
        }
        List<Method> methods = handlerClass.allMethods();
        Method dispatchMessage = null;
        for(Method method : methods){
            if(method.name().equals(methodName)){
                if(method.signature().equals(sig)){
                    if(dispatchMessage != null){
                        throw new IllegalStateException("");
                    }
                    dispatchMessage = method;
                }

            }
        }
        return dispatchMessage;


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
