package edu.colorado.plv.tracerunner_runtime_instrumentation;

import android.util.Log;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by s on 10/26/16.
 */

public class FrameworkResolver {
    private static FrameworkResolver instance = null;
    public static FrameworkResolver get(){
        if(instance ==null){
            instance = new FrameworkResolver();
        }
        return instance;
    }
    Pattern[] frameworkMatchers;
    FrameworkResolver(){
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
            frameworkMatchers[i] = Pattern.compile(createRegexFromGlob(globs[i]));
        }
    }
    boolean isFramework(Class clazz){
        String name = clazz.getName();
        return isFramework(name);
    }

    boolean isFramework(String name) {
        for(Pattern p:frameworkMatchers){
            if(p.matcher(name).find()){
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
            if (isFramework(iclass)) {
                return iclass;
            }
            superc = iclass.getSuperclass();
            iclass = superc;
        }while(superc != null);
        return null;
    }

    /**
     * modified from apache reflection library "ClassUtils.java"
     * this is a version which only gives the first override instead of the whole hierarchy
     * @param method Method to be checked if is overridden
     * @param interfacesBehavior Whether to include interfaces in the search
     * @return null if not overridden otherwise method reference
     */
    public Method getOverrideHierarchy(final Method method, final ClassUtils.Interfaces interfacesBehavior) {
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
            final Method m = getMatchingAccessibleMethod(c, method.getName(), parameterTypes);
            if (m == null) {
                continue;
            }
            if (Arrays.equals(m.getParameterTypes(), parameterTypes)) {
                // matches without generics
                if(isFramework(m.getDeclaringClass())) {
                    result.add(m);
                    continue;
                }
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
            if(isFramework(m.getDeclaringClass())) {
                result.add(m);
                break;
            }
        }
        if(result.size() == 0){
            return null;
        }
        return result.iterator().next();
    }
    /**
     * <p>Finds an accessible method that matches the given name and has compatible parameters.
     * Compatible parameters mean that every method parameter is assignable from
     * the given parameters.
     * In other words, it finds a method with the given name
     * that will take the parameters given.</p>
     *
     * <p>This method is used by
     * {
     * #invokeMethod(Object object, String methodName, Object[] args, Class[] parameterTypes)}.
     * </p>
     *
     * <p>This method can match primitive parameter by passing in wrapper classes.
     * For example, a {@code Boolean} will match a primitive {@code boolean}
     * parameter.
     * </p>
     *
     * @param cls find method in this class
     * @param methodName find method with this name
     * @param parameterTypes find method with most compatible parameters
     * @return The accessible method
     */
    public static Method getMatchingAccessibleMethod(final Class<?> cls,
                                                     final String methodName, final Class<?>... parameterTypes) {
        try {
            final Method method = cls.getDeclaredMethod(methodName, parameterTypes);
//            setAccessibleWorkaround(method);
            return method;
        } catch (final NoSuchMethodException e) { // NOPMD - Swallow the exception
        }
        // search through all methods
        Method bestMatch = null;
        final Method[] methods = cls.getDeclaredMethods();  //Shawn modification to get protected overridden methods
        for (final Method method : methods) {
            // compare name and parameters
            if (method.getName().equals(methodName) &&
                    isMatchingMethod(method, parameterTypes)) {
                // get accessible version of method
                final Method accessibleMethod = MethodUtils.getAccessibleMethod(method);
                if (accessibleMethod != null && (bestMatch == null || compareMethodFit(
                        accessibleMethod,
                        bestMatch,
                        parameterTypes) < 0)) {
                    bestMatch = accessibleMethod;
                }
            }
        }
//        if (bestMatch != null) {
//            setAccessibleWorkaround(bestMatch);
//        }
        return bestMatch;
    }
    static boolean isMatchingMethod(Method method, Class<?>[] parameterTypes) {
        return isMatchingExecutable(FrameworkResolver.Executable.of(method), parameterTypes);
    }
    static int compareMethodFit(final Method left, final Method right, final Class<?>[] actual) {
        return compareParameterTypes(Executable.of(left), Executable.of(right), actual);
    }
    private static int compareParameterTypes(final Executable left, final Executable right, final Class<?>[] actual) {
        final float leftCost = getTotalTransformationCost(actual, left);
        final float rightCost = getTotalTransformationCost(actual, right);
        return leftCost < rightCost ? -1 : rightCost < leftCost ? 1 : 0;
    }
    private static float getTotalTransformationCost(final Class<?>[] srcArgs, final Executable executable) {
        final Class<?>[] destArgs = executable.getParameterTypes();
        final boolean isVarArgs = executable.isVarArgs();

        // "source" and "destination" are the actual and declared args respectively.
        float totalCost = 0.0f;
        final long normalArgsLen = isVarArgs ? destArgs.length-1 : destArgs.length;
        if (srcArgs.length < normalArgsLen) {
            return Float.MAX_VALUE;
        }
        for (int i = 0; i < normalArgsLen; i++) {
            totalCost += getObjectTransformationCost(srcArgs[i], destArgs[i]);
        }
        if (isVarArgs) {
            // When isVarArgs is true, srcArgs and dstArgs may differ in length.
            // There are two special cases to consider:
            final boolean noVarArgsPassed = srcArgs.length < destArgs.length;
            final boolean explicitArrayForVarags = srcArgs.length == destArgs.length && srcArgs[srcArgs.length-1].isArray();

            final float varArgsCost = 0.001f;
            Class<?> destClass = destArgs[destArgs.length-1].getComponentType();
            if (noVarArgsPassed) {
                // When no varargs passed, the best match is the most generic matching type, not the most specific.
                totalCost += getObjectTransformationCost(destClass, Object.class) + varArgsCost;
            }
            else if (explicitArrayForVarags) {
                Class<?> sourceClass = srcArgs[srcArgs.length-1].getComponentType();
                totalCost += getObjectTransformationCost(sourceClass, destClass) + varArgsCost;
            }
            else {
                // This is typical varargs case.
                for (int i = destArgs.length-1; i < srcArgs.length; i++) {
                    Class<?> srcClass = srcArgs[i];
                    totalCost += getObjectTransformationCost(srcClass, destClass) + varArgsCost;
                }
            }
        }
        return totalCost;
    }
    private static final Class<?>[] ORDERED_PRIMITIVE_TYPES = { Byte.TYPE, Short.TYPE,
            Character.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE };
    private static float getPrimitivePromotionCost(final Class<?> srcClass, final Class<?> destClass) {
        float cost = 0.0f;
        Class<?> cls = srcClass;
        if (!cls.isPrimitive()) {
            // slight unwrapping penalty
            cost += 0.1f;
            cls = ClassUtils.wrapperToPrimitive(cls);
        }
        for (int i = 0; cls != destClass && i < ORDERED_PRIMITIVE_TYPES.length; i++) {
            if (cls == ORDERED_PRIMITIVE_TYPES[i]) {
                cost += 0.1f;
                if (i < ORDERED_PRIMITIVE_TYPES.length - 1) {
                    cls = ORDERED_PRIMITIVE_TYPES[i + 1];
                }
            }
        }
        return cost;
    }
    private static float getObjectTransformationCost(Class<?> srcClass, final Class<?> destClass) {
        if (destClass.isPrimitive()) {
            return getPrimitivePromotionCost(srcClass, destClass);
        }
        float cost = 0.0f;
        while (srcClass != null && !destClass.equals(srcClass)) {
            if (destClass.isInterface() && ClassUtils.isAssignable(srcClass, destClass)) {
                // slight penalty for interface match.
                // we still want an exact match to override an interface match,
                // but
                // an interface match should override anything where we have to
                // get a superclass.
                cost += 0.25f;
                break;
            }
            cost++;
            srcClass = srcClass.getSuperclass();
        }
        /*
         * If the destination class is null, we've traveled all the way up to
         * an Object match. We'll penalize this by adding 1.5 to the cost.
         */
        if (srcClass == null) {
            cost += 1.5f;
        }
        return cost;
    }
    private static boolean isMatchingExecutable(Executable method, Class<?>[] parameterTypes) {
        final Class<?>[] methodParameterTypes = method.getParameterTypes();
        if (method.isVarArgs()) {
            int i;
            for (i = 0; i < methodParameterTypes.length - 1 && i < parameterTypes.length; i++) {
                if (!ClassUtils.isAssignable(parameterTypes[i], methodParameterTypes[i], true)) {
                    return false;
                }
            }
            Class<?> varArgParameterType = methodParameterTypes[methodParameterTypes.length - 1].getComponentType();
            for (; i < parameterTypes.length; i++) {
                if (!ClassUtils.isAssignable(parameterTypes[i], varArgParameterType, true)) {
                    return false;
                }
            }
            return true;
        }
        return ClassUtils.isAssignable(parameterTypes, methodParameterTypes, true);
    }
    private static final class Executable {
        private final Class<?>[] parameterTypes;
        private final boolean  isVarArgs;

        private static FrameworkResolver.Executable of(Method method) { return new FrameworkResolver.Executable(method); }
        private static FrameworkResolver.Executable of(Constructor<?> constructor) { return new FrameworkResolver.Executable(constructor); }

        private Executable(Method method) {
            parameterTypes = method.getParameterTypes();
            isVarArgs = method.isVarArgs();
        }

        private Executable(Constructor<?> constructor) {
            parameterTypes = constructor.getParameterTypes();
            isVarArgs = constructor.isVarArgs();
        }

        public Class<?>[] getParameterTypes() { return parameterTypes; }

        public boolean isVarArgs() { return isVarArgs; }
    }
//    static boolean setAccessibleWorkaround(final AccessibleObject o) {
//        if (o == null || o.isAccessible()) {
//            return false;
//        }
//        final Member m = (Member) o;
//        //(modifiers & ACCESS_TEST) == 0
//        if (!o.isAccessible() && Modifier.isPublic(m.getModifiers()) && ((m.getDeclaringClass().getModifiers()) & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE)) == 0) {
//            try {
//                o.setAccessible(true);
//                return true;
//            } catch (final SecurityException e) { // NOPMD
//                // ignore in favor of subsequent IllegalAccessException
//            }
//        }
//        return false;
//    }

    Map<Class,Map<String,Method>> memTable = new HashMap<>();
    List<Method> getFrameworkOverrideMemo(Class clazz, String name, String[] argTypes) throws ClassNotFoundException{
        return getFrameworkOverride(clazz,name,argTypes); //TODO: figure out memoization if slow
//        if(memTable.containsKey(clazz)){
//            Map<String, Method> stringMethodMap = memTable.get(clazz);
//            if(stringMethodMap.containsKey(name)){
//                return stringMethodMap.get(name);
//            }else{
//                Method fo = getFrameworkOverride(clazz, name, argTypes);
//                stringMethodMap.put(name,fo);
//                return fo;
//            }
//        }else{
//            HashMap<String, Method> stringMethodMap = new HashMap<>();
//            memTable.put(clazz, stringMethodMap);
//            Method fo = getFrameworkOverride(clazz,name, argTypes);
//            stringMethodMap.put(name,fo);
//            return fo;
//        }
    }
    List<Method> getOverriddenMethods(Method method){
        Class<?> declaringClass = method.getDeclaringClass();

        List<Method> out = new ArrayList<>();
        int argCount = method.getParameterTypes().length;


        Class<?> superClass = declaringClass;
        do{
            Class<?>[] interfaces = superClass.getInterfaces();
            for(Class interfacei : interfaces){
                for (Method method1 : interfacei.getMethods()) {
                    addIfSuper(method, out, argCount, method1);
                }
            }

            superClass = superClass.getSuperclass();
            if(superClass != null) {
                for (Method method1 : superClass.getDeclaredMethods()) {
                    addIfSuper(method, out, argCount, method1);
                }
            }

        }while(superClass != null);

        return out;
    }

    private void addIfSuper(Method toMatch, List<Method> out, int argCount, Method toCheck) {
        if(toCheck.getParameterTypes().length == argCount){ //First pass ignore if arg count is different for speed
            if(toCheck.getName().equals(toMatch.getName())){
                if(isFramework(toCheck.getDeclaringClass())) {
                    boolean parametersMatch = true;
                    for (int i = 0; i < argCount; ++i) {
                        parametersMatch = parametersMatch && ClassUtils.isAssignable(toMatch.getParameterTypes()[i], toCheck.getParameterTypes()[i]);

                        //toMatch.getParameterTypes()[i].equals(toCheck.getParameterTypes()[i]);
                    }
                    if (parametersMatch) {
                        out.add(toCheck);
                    }
                }
            }
        }
    }

    List<Method> getFrameworkOverride(Class clazz, String name, String[] argTypes) throws ClassNotFoundException {
        Method method1 = null;
        Class[] objects = null;
        try {
            objects = new Class[argTypes.length];
            for(int j = 0; j< argTypes.length; ++j){
                objects[j] = ClassUtils.getClass(argTypes[j]);
            }
            Class cclazz = clazz;
            while(method1 == null) {
                try {
                    method1 = cclazz.getDeclaredMethod(name, objects); //issue #31
                }catch(NoSuchMethodException e){
                    cclazz = cclazz.getSuperclass();
                    if(cclazz == null){
                        throw e;
                    }
                }
            }
        } catch (NoSuchMethodException e) {

            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                int modifiers = method.getModifiers();
                Class<?>[] parameterTypes = method.getParameterTypes();
                String typesSer = "[";
                for (Class parameterType : parameterTypes) {
                    typesSer += parameterType.getName();
                }
                typesSer += "]";
                Log.e("TraceRunnerInst", "class: " + clazz.getName() + " method: " + method.getName() + " args: " + typesSer + " modifiers: " + modifiers);
            }

            String objectsSer = "[";
            for (Object object : objects) {
                objectsSer += object.toString() + ",";
            }
            objectsSer += "]";

            String argSer = "[";
            for (Object argType : argTypes) {
                argSer += argType.toString() + ",";
            }
            argSer += "]";
            throw new RuntimeException("methodName: " + name + " objects: "
                    + objectsSer + " types: " + argSer, e);
        }
//        return  getOverrideHierarchy(method1, ClassUtils.Interfaces.INCLUDE);
        return getOverriddenMethods(method1);
    }
    public static String sootSignatureFromJava(Method method){
        String s = method.getReturnType().getName() + " " +  method.getName() + "(";
        boolean first = true;
        for (Class<?> parameter : method.getParameterTypes()) {
            if(!first){
                s += ",";
            }
            s += parameter.getName();
            first = false;
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
}
