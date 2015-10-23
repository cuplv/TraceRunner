package edu.colorado.plv;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by s on 10/22/15.
 * TODO: probably encode this in a resource file at some point
 */
public class ClassExclusions {
    public static List<String> getClassExlusions() {
        List<String> exclusions = new ArrayList<>();
        exclusions.add("android.media.MediaHTTPConnection");
        exclusions.add("android.util.Log");
        return exclusions;
    }
    public static List<String> exceptionExclusions() {
        List<String> exclusions = new ArrayList<>();
        exclusions.add("java.lang.ClassNotFoundException");
        return exclusions;
    }
}
