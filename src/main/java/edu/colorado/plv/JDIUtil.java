package edu.colorado.plv;

import com.sun.jdi.ThreadReference;

/**
 * Created by s on 11/2/15.
 */
public class JDIUtil {
    public static boolean threadCmp(ThreadReference t1, ThreadReference t2){
        boolean idseq = t1.uniqueID() == t2.uniqueID();
        return idseq;
    }
}
