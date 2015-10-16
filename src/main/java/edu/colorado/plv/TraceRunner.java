package edu.colorado.plv;
import java.io.FileInputStream;
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
        if (args.length == 1) {
            FileInputStream fileInputStream = new FileInputStream(args[0]);
            ProtoProcessor.readInputStream(fileInputStream);
            return;

        }
        if (args.length < 3){
            throw new IllegalArgumentException("usage: [port] [log output file] [class filter] or [log input file]");
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



}
