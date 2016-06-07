package edu.colorado.plv;

import com.sun.jdi.*;
import com.sun.jdi.event.ExceptionEvent;

/**
 * Created by s on 12/5/15.
 */
public class ExceptionCache {
    private final String sourceName;
    private boolean exceptionInfo;
    private final long uniqueID;
    private final CallbackOuterClass.PValue exception;
    private final String name;
    private final String declaringType;
    private final int lineNumber;



    ExceptionCache(ExceptionEvent evt, ThreadReference altThreadReference){
        if(evt != null) {
            exceptionInfo = true;
            Location location = evt.location();
            this.lineNumber = location.lineNumber();
            String iSourceName;
            try {
                iSourceName = location.sourceName();
            } catch (AbsentInformationException e) {
                iSourceName = "<<None>>";
            }
            sourceName = iSourceName;

            ThreadReference threadReference = evt.thread();
            Method method = location.method();
            ObjectReference exception1 = evt.exception();

            this.exception = ProtoProcessor.valueToProtobuf(exception1, true);
            this.name = method.name();
            this.declaringType = method.declaringType().name();
            this.uniqueID = threadReference.uniqueID();


        }else {
            exceptionInfo = false;
            exception = null;
            name = null;
            declaringType = null;
            lineNumber = -1;
            sourceName = null;
            this.uniqueID = altThreadReference.uniqueID();
        }
    }

    public boolean isExceptionInfo() {
        return exceptionInfo;
    }

    public long getUniqueID() {
        return uniqueID;
    }

    public CallbackOuterClass.PValue getException() {
        return exception;
    }

    public String getName() {
        return name;
    }

    public String getDeclaringType() {
        return declaringType;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getSourceName() {
        return sourceName;
    }
}
