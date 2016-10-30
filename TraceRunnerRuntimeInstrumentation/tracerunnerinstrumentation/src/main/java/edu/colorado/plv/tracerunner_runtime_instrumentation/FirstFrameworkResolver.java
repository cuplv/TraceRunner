package edu.colorado.plv.tracerunner_runtime_instrumentation;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    public static Method getOverrideHierarchy(final Method method, final ClassUtils.Interfaces interfacesBehavior) {
        Validate.notNull(method);
        final Set<Method> result = new LinkedHashSet<Method>();
        //result.add(method);

        final Class<?>[] parameterTypes = method.getParameterTypes();

        final Class<?> declaringClass = method.getDeclaringClass();

        final Iterator<Class<?>> hierarchy = ClassUtils.hierarchy(declaringClass, interfacesBehavior).iterator();
        //skip the declaring class :P
        hierarchy.next();
        hierarchyTraversal: while (hierarchy.hasNext()) {
            final Class<?> c = hierarchy.next();
            final Method m = MethodUtils.getMatchingAccessibleMethod(c, method.getName(), parameterTypes);
            if (m == null) {
                continue;
            }
            if (Arrays.equals(m.getParameterTypes(), parameterTypes)) {
                // matches without generics
                result.add(m);
                continue;
            }
            // necessary to get arguments every time in the case that we are including interfaces
            final Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(declaringClass, m.getDeclaringClass());
            for (int i = 0; i < parameterTypes.length; i++) {
                final Type childType = TypeUtils.unrollVariables(typeArguments, method.getGenericParameterTypes()[i]);
                final Type parentType = TypeUtils.unrollVariables(typeArguments, m.getGenericParameterTypes()[i]);
                if (!TypeUtils.equals(childType, parentType)) {
                    continue hierarchyTraversal;
                }
            }
            result.add(m);
            break;
        }
        if(result.size() == 0){
            return null;
        }
        return result.iterator().next();
    }
    Method getFrameworkOverride(Class clazz, String name){
        //TODO: handle generics
        //TODO: handle superclass


//        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();


        String[] parsedSig = Strings.extractMethodSignature(name.split(" ")[1]);
        Method method1 = null;
        try {
            List<Class> args = new ArrayList<Class>();
            for(int i = 1; i < parsedSig.length; ++i){
                if(parsedSig[i].equals( ""))
                    break;
                try {
                    args.add(Class.forName(parsedSig[i]));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }

            Class[] objects = new Class[args.size()];
            for(int j = 0; j< args.size(); ++j){
                objects[j] = args.get(j);
            }
            method1 = clazz.getMethod(parsedSig[0], objects);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
//        MethodUtils.getOverrideHierarcy(null, ClassUtils.Interfaces.)
        Method classesWithMethod = getOverrideHierarchy(method1, ClassUtils.Interfaces.INCLUDE);
//        Class[] interfaces = clazz.getInterfaces();
//        List<Class> classesWithMethod = new ArrayList<>();
//        for(Class intf: interfaces){
//            Method[] methods = intf.getMethods();
//            for(Method method : methods){
//                String methodName = method.getName();
//                Type[] genericParameterTypes = method.getGenericParameterTypes();
//                Class<?> returnType = method.getReturnType();
//
//                String methodSig = returnType.getName() + " " + methodName + "(";
//                for(Type param: genericParameterTypes){
//                    methodSig += param.toString();
//                }
//                methodSig += ")";
//                if(methodSig.equals(name)){
//                    classesWithMethod.add(intf);
//                    break;
//                }
//            }
//        }

        return classesWithMethod;
    }
    public static String sootSignatureFromJava(Method method){
        String s = method.getReturnType().getName() + " " +  method.getName() + "(";
        boolean first = true;
        for (Class<?> parameter : method.getParameterTypes()) {
            if(!first){
                s += ",";
            }
            s += parameter.toString();
        }
        s += ")";

        return s;
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
    public void parseJavaSignature(String sig){

    }
}
