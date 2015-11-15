package edu.colorado.plv;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by s on 10/22/15.
 * TODO: probably encode this in a resource file at some point
 */
public class Resources {
    public static List<String> getClassExlusions() {
        List<String> exclusions = new ArrayList<>();
        exclusions.add("android.media.MediaHTTPConnection");
        exclusions.add("android.util.Log");
        exclusions.add("android.os.Parcel");
        exclusions.add("android.content.res.*");
        exclusions.add("android.util.SparseArray");
        exclusions.add("android.util.LongSparseArray");
        exclusions.add("android.animation.*");
        exclusions.add("android.graphics.*");
        exclusions.add("com.google.inject.*");
        return exclusions;
    }
    public static List<String> exceptionExclusions() {
        List<String> exclusions = new ArrayList<>();
        exclusions.add("java.lang.ClassNotFoundException");
        return exclusions;
    }
    public static List<String> getLogeSigs(){
        List<String> sigs = new ArrayList<>();
        sigs.add("(Ljava/lang/String;Ljava/lang/String;)I");
        sigs.add("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I");
        return sigs;
    }

    public static List<String> getWSigs() {
        List<String> sigs = new ArrayList<>();
        sigs.add("(Ljava/lang/String;Ljava/lang/String;)I");
        sigs.add("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I");
        sigs.add("(Ljava/lang/String;Ljava/lang/Throwable;)I");
        return sigs;
    }

    public static List<String> getWTFSigs() {
        List<String> sigs = new ArrayList<>();
        sigs.add("(ILjava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;ZZ)I");
        sigs.add("(Ljava/lang/String;Ljava/lang/String;)I");
        sigs.add("Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I");
        sigs.add("(Ljava/lang/String;Ljava/lang/Throwable;)I");
        return sigs;
    }
}
