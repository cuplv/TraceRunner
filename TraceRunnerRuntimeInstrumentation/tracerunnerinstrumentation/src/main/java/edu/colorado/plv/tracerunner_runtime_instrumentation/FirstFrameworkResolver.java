package edu.colorado.plv.tracerunner_runtime_instrumentation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by s on 10/26/16.
 */

public class FirstFrameworkResolver {

    Pattern[] frameworkMatchers;
    FirstFrameworkResolver(){
        String[] globs = new String[]{
                "android.*" ,
                "com.android.*" ,
                "com.google.*" ,
                "dalvik.*" ,
                "java.*" ,
                "javax.*" ,
                "junit.*" ,
                "org.apache.http.*" ,
                "org.json.*" ,
                "org.w3c.dom.*" ,
                "org.xml.sax.*" ,
                "org.xmlpull.v1.*" ,
                "org.hamcrest.*" ,
                "com.squareup.javawriter.*"};
        frameworkMatchers = new Pattern[globs.length];
        for(int i = 0; i< globs.length; ++i){
            frameworkMatchers[i] = Pattern.compile(globs[i]);
        }
    }
    boolean classMatches(Class clazz){
        for(Pattern p:frameworkMatchers){
            if(p.matcher(clazz.getName()).find()){
                return true;
            }
        }
        return false;
    }

    /**
     * Compute the most precise object in the class hierarchy which is in the framework
     * If class doesn't extend anything this will return java.lang.Object
     * Don't call this directly, use memoization function
     * @param clazz
     * @return
     */
    Class getFirstFrameworkClass(Class clazz){
        Class iclass = clazz;
        Class superc;
        do {
            if (classMatches(iclass)) {
                return iclass;
            }
            superc = iclass.getSuperclass();
            iclass = superc;
        }while(superc != null);
        return null;
    }
    Class getFrameworkOverride(Class clazz, String name){
        clazz.getInterfaces();


        return null;
    }
    public static String createRegexFromGlob(String glob)
    {
        String out = "^";
        for(int i = 0; i < glob.length(); ++i)
        {
            final char c = glob.charAt(i);
            switch(c)
            {
                case '*': out += ".*"; break;
                case '?': out += '.'; break;
                case '.': out += "\\."; break;
                case '\\': out += "\\\\"; break;
                default: out += c;
            }
        }
        out += '$';
        return out;
    }
}
