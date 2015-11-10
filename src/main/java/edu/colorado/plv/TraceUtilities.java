package edu.colorado.plv;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by s on 11/10/15.
 */
public class TraceUtilities {
    public static List<CallbackOuterClass.EventInCallback> deserialize(FileInputStream fileInputStream)
            throws IOException {
        List<CallbackOuterClass.EventInCallback> out = new ArrayList<>();
        while(true) {
            CallbackOuterClass.EventInCallback callback =
                    CallbackOuterClass.EventInCallback.parseDelimitedFrom(fileInputStream);
            if(callback != null){
                out.add(callback);
            }else{
                return out;
            }
        }

    }
}
