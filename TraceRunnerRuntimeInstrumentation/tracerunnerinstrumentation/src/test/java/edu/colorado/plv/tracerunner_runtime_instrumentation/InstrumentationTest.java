package edu.colorado.plv.tracerunner_runtime_instrumentation;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Formattable;
import java.util.Formatter;
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
        Method frameworkOverride = f.getFrameworkOverrideMemo(r.getClass(), "void run()");
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
        Method frameworkOverride = f.getFrameworkOverrideMemo(c.getClass(), "java.lang.Object call()");
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
        Method frameworkOverride = f.getFrameworkOverrideMemo(c.getClass(), "java.lang.String call()");
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
//        String m = c.getClass().getMethods()[0].toString();
        Method frameworkOverride = f.getFrameworkOverrideMemo(c.getClass(),
                "boolean equals(java.lang.Object)");
        assertEquals("interface java.util.Comparator",frameworkOverride.getDeclaringClass().toString());
        assertEquals("boolean equals(java.lang.Object)", FirstFrameworkResolver.sootSignatureFromJava(frameworkOverride));

        Method m = f.getFrameworkOverrideMemo(c.getClass(), "int compare(java.lang.String,java.lang.String)");
        assertEquals("java.util.Comparator", m.getDeclaringClass().getName());

    }
    @Test
    public void nonFrameworkOverride() throws Exception{
        FirstFrameworkResolver f = new FirstFrameworkResolver();
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
        Method m = f.getFrameworkOverrideMemo(b.getClass(), "void bar(int)");
        assertEquals(null,m);
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
        FirstFrameworkResolver f = new FirstFrameworkResolver();
        MyObject o = new MyObject();
        Method m = f.getFrameworkOverrideMemo(o.getClass(), "java.lang.String toString()");
        assertEquals("java.lang.Object", m.getDeclaringClass().getName());
        assertEquals("java.lang.String toString()", FirstFrameworkResolver.sootSignatureFromJava(m));
        Method m2 = f.getFrameworkOverrideMemo(o.getClass(), "boolean equals(java.lang.Object)");
        assertEquals("java.lang.Object", m2.getDeclaringClass().getName());
        assertEquals("boolean equals(java.lang.Object)", FirstFrameworkResolver.sootSignatureFromJava(m2));
    }
    @Test
    public void formattableTest() throws Exception{
        FirstFrameworkResolver f = new FirstFrameworkResolver();
        Formattable fmt = new Formattable(){

            @Override
            public void formatTo(Formatter formatter, int flags, int width, int precision) {

            }
        };
        Method m = f.getFrameworkOverrideMemo(fmt.getClass(), "void formatTo(java.util.Formatter,int,int,int)");
        assertEquals("java.util.Formattable", m.getDeclaringClass().getName());
        assertEquals("void formatTo(java.util.Formatter,int,int,int)",FirstFrameworkResolver.sootSignatureFromJava(m));
        Method m2 = f.getFrameworkOverrideMemo(fmt.getClass(), "void formatTo(java.util.Formatter,int,int,int)");
        assertEquals("java.util.Formattable", m2.getDeclaringClass().getName());
        assertEquals("void formatTo(java.util.Formatter,int,int,int)",FirstFrameworkResolver.sootSignatureFromJava(m2));
    }

}