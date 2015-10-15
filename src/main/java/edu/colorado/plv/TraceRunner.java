package edu.colorado.plv;
import java.io.FileOutputStream;
import java.util.*;


/**
 * Created by s on 8/11/15.
 * This is the record everything trace runner, it will probably be too slow and need refactoring.
 * TODO: In progress
 */
public class TraceRunner {
    public static void main(String[] args) throws Exception
    {
        if (args.length < 3){
            throw new IllegalArgumentException("usage: [port] [log output file] [class filter] or [log input file]");
        }
        if (args.length == 1) {


        }
        String logOutput = args[1];
        int port = Integer.parseInt(args[0]);
        List<String> filters = new ArrayList<>();
        for(int j=2; j< args.length; ++j){
            filters.add(args[j]);
        }
        EventProcessor eventProcessor = new ProtoProcessor(new FileOutputStream(logOutput));
        TraceMainLoop traceMainLoop = new TraceMainLoop(true, port,  eventProcessor, filters);
        traceMainLoop.mainLoop();
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



}
