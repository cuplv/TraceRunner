package edu.colorado.plv.tracerunner_runtime_instrumentation;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Formattable;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class InstrumentationTest {
    public static final String METHOD_NAME = Thread.currentThread().getStackTrace()[0].getMethodName();

    //    @Test
//    public void addition_isCorrect() throws Exception {
//        assertEquals(4, 2 + 2);
//    }
    @Test
    public void firstFrameworkSuperTest() throws Exception{
        FrameworkResolver f = new FrameworkResolver();
        Runnable r = new Runnable() {
            @Override
            public void run() {

            }
        };
        Class<? extends Runnable> aClass = r.getClass();
        Class firstFrameworkClass = f.getFirstFrameworkClass(aClass);
        assertEquals("java.lang.Object",firstFrameworkClass.getName());
    }
    @Test
    public void frameworkOverrideTest() throws Exception{
        FrameworkResolver f = new FrameworkResolver();
        Runnable r = new Runnable() {
            @Override
            public void run() {

            }
        };
        Method frameworkOverride = f.getFrameworkOverrideMemo(r.getClass(), "run", new String[0]).iterator().next();
//        assertEquals(1,frameworkOverride.size());
        assertEquals("void run()", FrameworkResolver.sootSignatureFromJava(frameworkOverride));
        assertEquals("interface java.lang.Runnable", frameworkOverride.getDeclaringClass().toString());
    }
    @Test
    public void frameworkOverrideParameterTest() throws Exception{
        FrameworkResolver f = new FrameworkResolver();
        Callable<Object> c = new Callable() {
            @Override
            public Object call(){
                return new Object();
            }
        };
        Method frameworkOverride = f.getFrameworkOverrideMemo(c.getClass(), "call", new String[0]).iterator().next();
//        assertEquals(1,frameworkOverride.size());
        assertEquals("java.lang.Object call()", FrameworkResolver.sootSignatureFromJava(frameworkOverride));
        assertEquals("interface java.util.concurrent.Callable", frameworkOverride.getDeclaringClass().toString());
    }
    @Test
    public void frameworkOverrideParameterTestSubclass() throws Exception{
        FrameworkResolver f = new FrameworkResolver();
        Callable<String> c = new Callable() {
            @Override
            public String call(){
                return "hi";
            }
        };
        Method frameworkOverride = f.getFrameworkOverrideMemo(c.getClass(), "call", new String[0]).iterator().next();
        assertEquals("interface java.util.concurrent.Callable", frameworkOverride.getDeclaringClass().toString());
        assertEquals("java.lang.Object call()", FrameworkResolver.sootSignatureFromJava(frameworkOverride));
    }
    @Test
    public void frameworkOverrideArgsTest() throws Exception{
        FrameworkResolver f = new FrameworkResolver();
        Comparator<String> c = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return 0;
            }

            @Override
            public boolean equals(Object obj) {
                return false;
            }
        };
//        String m = c.getClass().getMethods()[0].toString();
        Method frameworkOverride = f.getFrameworkOverrideMemo(c.getClass(),
                "equals", new String[]{"java.lang.Object"}).iterator().next();
        assertEquals("interface java.util.Comparator",frameworkOverride.getDeclaringClass().toString());
        assertEquals("boolean equals(java.lang.Object)", FrameworkResolver.sootSignatureFromJava(frameworkOverride));

        List<Method> overrides = f.getFrameworkOverrideMemo(c.getClass(), "compare", new String[]{"java.lang.String", "java.lang.String"});
        Method m = overrides.iterator().next();
        assertEquals("java.util.Comparator", m.getDeclaringClass().getName());

    }
    @Test
    public void initOverrideTest() throws Exception{
        FrameworkResolver f = new FrameworkResolver();
        class MyException extends Exception{
            MyException(){
                System.out.println("hi there");
            }
        }
        MyException myException = new MyException();
        Class<?> superclass = myException.getClass().getSuperclass();
        Method[] methods = myException.getClass().getMethods();
        Constructor<?>[] constructors = myException.getClass().getConstructors();
        List<Method> overrides = f.getFrameworkOverrideMemo(myException.getClass(), "<init>", new String[0]);
        Method m = overrides.iterator().next();
        assertEquals("java.lang.Exception", m.getDeclaringClass().getName());
    }
    @Test
    public void nonFrameworkOverride() throws Exception{
        FrameworkResolver f = new FrameworkResolver();
        class Foo{
            public void bar(){}
            public void bar(int i){}
        }
        class Bar extends Foo{
            @Override
            public void bar(int i){

            }
        }
        Bar b = new Bar();
        List<Method> supers = f.getFrameworkOverrideMemo(b.getClass(), "bar", new String[]{"int"});
        assertEquals(0,supers.size());
//        Method m = supers.iterator().next();
//        assertEquals(null,m);
    }
    public class MyObject extends Object{
        @Override
        public String toString(){
            return "hi";
        }
        @Override
        public boolean equals(Object o){
            return false;
        }
    }
    @Test
    public void extendsTest() throws Exception{
        FrameworkResolver f = new FrameworkResolver();
        MyObject o = new MyObject();
        Method m = f.getFrameworkOverrideMemo(o.getClass(), "toString", new String[0]).iterator().next();
        assertEquals("java.lang.Object", m.getDeclaringClass().getName());
        assertEquals("java.lang.String toString()", FrameworkResolver.sootSignatureFromJava(m));
        Method m2 = f.getFrameworkOverrideMemo(o.getClass(), "equals", new String[]{"java.lang.Object"}).iterator().next();
        assertEquals("java.lang.Object", m2.getDeclaringClass().getName());
        assertEquals("boolean equals(java.lang.Object)", FrameworkResolver.sootSignatureFromJava(m2));
    }
    @Test
    public void formattableTest() throws Exception{
        FrameworkResolver f = new FrameworkResolver();
        Formattable fmt = new Formattable(){

            @Override
            public void formatTo(Formatter formatter, int flags, int width, int precision) {

            }
        };
        Method m = f.getFrameworkOverrideMemo(fmt.getClass(), "formatTo", new String[]{"java.util.Formatter","int","int","int"}).iterator().next();

        assertEquals("java.util.Formattable", m.getDeclaringClass().getName());
        assertEquals("void formatTo(java.util.Formatter,int,int,int)", FrameworkResolver.sootSignatureFromJava(m));
        Method m2 = f.getFrameworkOverrideMemo(fmt.getClass(), "formatTo", new String[]{"java.util.Formatter","int","int","int"}).iterator().next();
        assertEquals("java.util.Formattable", m2.getDeclaringClass().getName());
        assertEquals("void formatTo(java.util.Formatter,int,int,int)", FrameworkResolver.sootSignatureFromJava(m2));
    }
    @Test
    public void exceptionLogTest() throws Exception{
        try{
            throw new RuntimeException("e");
        }catch(Throwable e) {
            TraceRunnerRuntimeInstrumentation.logException(e, "foo","bar");
        }
    }

}