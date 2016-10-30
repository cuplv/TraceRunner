package edu.colorado.plv.tracerunner_runtime_instrumentation;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
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
        FirstFrameworkResolver f = new FirstFrameworkResolver();
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
        FirstFrameworkResolver f = new FirstFrameworkResolver();
        Runnable r = new Runnable() {
            @Override
            public void run() {

            }
        };
        Method frameworkOverride = f.getFrameworkOverride(r.getClass(), "void run()");
//        assertEquals(1,frameworkOverride.size());
        assertEquals("void run()", FirstFrameworkResolver.sootSignatureFromJava(frameworkOverride));
        assertEquals("interface java.lang.Runnable", frameworkOverride.getDeclaringClass().toString());
    }
    @Test
    public void frameworkOverrideParameterTest() throws Exception{
        FirstFrameworkResolver f = new FirstFrameworkResolver();
        Callable<Object> c = new Callable() {
            @Override
            public Object call(){
                return new Object();
            }
        };
        Method frameworkOverride = f.getFrameworkOverride(c.getClass(), "java.lang.Object call()");
//        assertEquals(1,frameworkOverride.size());
        assertEquals("java.lang.Object call()", FirstFrameworkResolver.sootSignatureFromJava(frameworkOverride));
        assertEquals("interface java.util.concurrent.Callable", frameworkOverride.getDeclaringClass().toString());
    }
    @Test
    public void frameworkOverrideParameterTestSubclass() throws Exception{
        FirstFrameworkResolver f = new FirstFrameworkResolver();
        Callable<String> c = new Callable() {
            @Override
            public String call(){
                return "hi";
            }
        };
        Method frameworkOverride = f.getFrameworkOverride(c.getClass(), "java.lang.String call()");
        assertEquals("interface java.util.concurrent.Callable", frameworkOverride.getDeclaringClass().toString());
        assertEquals("java.lang.Object call()", FirstFrameworkResolver.sootSignatureFromJava(frameworkOverride));
    }
    @Test
    public void frameworkOverrideArgsTest() throws Exception{
        FirstFrameworkResolver f = new FirstFrameworkResolver();
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
        String m = c.getClass().getMethods()[0].toString();
        Method frameworkOverride = f.getFrameworkOverride(c.getClass(),
                "boolean equals(java.lang.Object)");
        assertEquals("interface java.util.Comparator",frameworkOverride.getDeclaringClass().toString());
    }
//    @Test
//    public void sandbox() throws Exception{
//        class Foo{
//            public void bar(){}
//            public void bar(int i){}
//        }
//        Foo f = new Foo();
//        Class<? extends Foo> aClass = f.getClass();
//        Method[] methods = aClass.getMethods();
//        Method bar = aClass.getMethod("bar", methods[1].getParameterTypes()[0]);
//        System.out.println();
//
//    }
}