package edu.colorado.plv;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.request.BreakpointRequest;

import java.util.List;
import java.util.Map;

/**
 * Created by s on 10/13/15.
 */
public interface EventProcessor {
    //Map<String, Value> processEvent(Event evt, List<String> mentered) throws AbsentInformationException, IncompatibleThreadStateException;
    void processMessage(BreakpointEvent evt) throws IncompatibleThreadStateException, AbsentInformationException;
    void processInvoke(MethodEntryEvent evt);
    void done();
    void processMethodExit(MethodExitEvent evt);
}
