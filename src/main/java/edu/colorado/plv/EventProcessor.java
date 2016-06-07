package edu.colorado.plv;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.event.*;

import java.util.regex.Pattern;

/**
 * Created by s on 10/13/15.
 */
public interface EventProcessor {
    void setAppPackageRegex(Pattern appPackageRegex);
    //Map<String, Value> processEvent(Event evt, List<String> mentered) throws AbsentInformationException, IncompatibleThreadStateException;
    void processMessage(BreakpointEvent evt) throws IncompatibleThreadStateException, AbsentInformationException;
    void processErrorLog(BreakpointEvent evt, String type) throws IncompatibleThreadStateException, AbsentInformationException;
    void processInvoke(MethodEntryEvent evt, boolean isCallback, boolean isCallIn);
    void done();
    void processMethodExit(MethodExitEvent evt, boolean isCallback);

    void processException(ExceptionCache exceptionCache);
}
