package edu.tracerunner;
import java.util.*;

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
        if (args.length < 3){
            throw new IllegalArgumentException("usage: [port] [log output file] [class filter] [optional Read]");
        }
        if (args.length == 4) {


        }
        String logOutput = args[1];
        int port = Integer.parseInt(args[0]);
        List<String> filters = new ArrayList<>();
        for(int j=2; j< args.length; ++j){
            filters.add(args[j]);
        }
        EventProcessor eventProcessor = new PrintProcessor();
        TraceMain traceMain = new TraceMain(true, port,  eventProcessor, filters);
        traceMain.mainLoop();



    }


}
