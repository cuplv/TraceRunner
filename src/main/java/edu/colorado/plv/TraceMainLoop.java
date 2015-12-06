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
    //private boolean enabledAndroidStar = false;
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


            //get class hierarchy
            vm.allClasses();


            //Process breakpoints main loop
            Map<ThreadReference,Stack<Method>> callStack = new HashMap<>(); //Null until callback hit
            ThreadReference activityThread = null;
//            Method callIn = null;

            //Exception handling
            Map<ThreadReference, ExceptionCache> lastExceptionOnThread = new HashMap<>();


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
//                                    if(!(activityThread.equals(((BreakpointEvent) evt).thread()))){
//                                        throw new IllegalStateException("Looper looped from wrong thread");
//                                    }
                                    disableAll(((BreakpointEvent) evt).thread());
                                }
                                EventRequest request = evt.request();
                                if(request == bReqDispatch) {
                                    eventProcessor.processMessage((BreakpointEvent) evt);
                                    callStack.remove(thrf); //TODO: if additional breakpoints add check for msg here
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

                                Method m = mevt.method();
                                String name = m.declaringType().name();
                                Stack<Method> threadCallStack = callStack.get(thref);

                                if(threadCallStack == null || threadCallStack.size() == 0) {
                                    //setting callback
                                    if(appPackageRegex.matcher(name).matches()){
                                        //set callback value when app package is first hit
                                        enableAll(thref);
                                        threadCallStack = new Stack<>();
                                        callStack.put(thref, threadCallStack);
                                    }else{
                                        throw new IllegalStateException("should never happen");
                                    }
                                }


                                boolean isCallback = threadCallStack.size() == 0;

                                //TODO: check callin
                                if(headIsInPackage(threadCallStack)) {
                                    eventProcessor.processInvoke(mevt, isCallback, false);
                                }

                                threadCallStack.push(m);

                            }else if (evt instanceof MethodExitEvent){
                                MethodExitEvent mxe = (MethodExitEvent) evt;
                                ThreadReference thref = mxe.thread();
                                Method m = mxe.method();

                                Stack<Method> threadCallStack = callStack.get(thref);

                                boolean exnStartedInFramework = !headIsInPackage(threadCallStack);
                                boolean exnCaughtInApp = false;

                                Method pop = threadCallStack.pop();

                                //If an exception is thrown there will
                                while(!m.equals(pop) && threadCallStack.size() > 0){
                                    exnCaughtInApp = headIsInPackage(threadCallStack);
                                    pop = threadCallStack.pop();

                                }

                                if(exnStartedInFramework && exnCaughtInApp){
                                    ExceptionCache exn = lastExceptionOnThread.get(thref);
                                    eventProcessor.processException(exn);
                                }
//                                if(!m.equals(pop)){
//                                    throw new IllegalStateException(); //TODO: mismatched entry/exit bug
//                                }
                                boolean isCallback = threadCallStack.size() == 0;
                                if(isCallback) {
                                    disableAll(thref);
                                }

                                if(headIsInPackage(threadCallStack)) {
                                    eventProcessor.processMethodExit(mxe, isCallback);
                                }
                            }else if (evt instanceof ExceptionEvent){
                                ExceptionEvent exn = (ExceptionEvent) evt;
                                exn.location().method();
                                ThreadReference threadReference = exn.thread();
//                                Stack<Method> threadCallStack = callStack.get(threadReference);
                                lastExceptionOnThread.put(threadReference,
                                        new ExceptionCache(exn, threadReference));


                            }
                        } catch (Exception exc) {
                            exc.printStackTrace();
                            System.out.println(exc.getClass().getName() + ": " + exc.getMessage());
                            System.exit(-1);
                        } finally {
                            evtSet.resume();
                        }
                    }
                }
                //TODO: https://dzone.com/articles/generating-minable-event
            } catch (VMDisconnectedException e) {
                Set<ThreadReference> threadReferences = callStack.keySet();
                for (ThreadReference threadReference : threadReferences) {
                    Stack<Method> threadCallStack = callStack.get(threadReference);
                    if(threadCallStack.size() != 0){
                        ExceptionCache exceptionEvent = lastExceptionOnThread.get(threadReference);
                        eventProcessor.processException(exceptionEvent);
                    }
                }

                eventProcessor.done();
                System.out.println("Process exited");

            }
        }
    }

    /**
     *
     *
     * @param threadCallStack sparse call stack for android application
     * @return true if stack is empty
     * @return true if top of stack is in the application package
     * @return false if top of stack is in framework
     */
    private boolean headIsInPackage(Stack<Method> threadCallStack) {
        if(threadCallStack.size() < 1){
            return true;
        }
        Method head = threadCallStack.peek();
        ReferenceType referenceType = head.declaringType();
        String type = referenceType.toString().split(" ")[1];
        if(appPackageRegex.matcher(type).matches()){
            return true;
        }
        return false;

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
