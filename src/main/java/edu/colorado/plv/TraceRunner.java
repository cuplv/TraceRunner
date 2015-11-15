package edu.colorado.plv;
import javax.xml.crypto.Data;
import java.io.File;
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
        if (args.length == 0){
            System.out.println("usage: [port] [log output file] [class filter] or [log input file]\n" +
                    "usage: read [file]\n" +
                    "usage: dataProj [proto file] [directory] [appPackage glob]");

        }
        if (args[0].equals("dataProj")){
            FileInputStream fileInputStream = new FileInputStream(args[1]);
            DataProjection dataProjection = DataProjection.fromFileInputStream(fileInputStream, args[3]);
            for(CallbackOuterClass.PValue pValue :dataProjection.getInvolvedObjects()){
                System.out.println(pValue);
            }
            dataProjection.generateNestedTraces();
            dataProjection.expandToDirectory(new File(args[2]));
            return;

            //TraceUtilities.dataProjection
        }
        if (args[0].equals("read")) {
            FileInputStream fileInputStream = new FileInputStream(args[1]);
            ProtoProcessor.readInputStream(fileInputStream);
            return;

        }


        String logOutput = args[1];
        int port = Integer.parseInt(args[0]);
        List<String> filters = new ArrayList<>();
        String appPackage = args[2];
        for(int j=3; j< args.length; ++j){
            filters.add(args[j]);
        }
        EventProcessor eventProcessor = new ProtoProcessor(new FileOutputStream(logOutput));
        TraceMainLoop traceMainLoop = new TraceMainLoop(port,  eventProcessor, filters, appPackage);
        traceMainLoop.mainLoop();
    }



}
