package edu.colorado.plv.tracerunner_runtime_instrumentation;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class InstrumentationTest {
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
        List<Class> frameworkOverride = f.getFrameworkOverride(r.getClass(), "void run()");
        assertEquals(1,frameworkOverride.size());
        assertEquals("java.lang.Runnable", frameworkOverride.get(0).getName());
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
        List<Class> frameworkOverride = f.getFrameworkOverride(c.getClass(), "java.lang.Object call()");
        assertEquals(1,frameworkOverride.size());
        assertEquals("java.util.concurrent.Callable", frameworkOverride.get(0).getName());
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
        List<Class> frameworkOverride = f.getFrameworkOverride(c.getClass(), "java.lang.String call()");
        assertEquals(1,frameworkOverride.size());
        assertEquals("java.util.concurrent.Callable", frameworkOverride.get(0).getName());
    }
    @Test
    public void sandbox() throws Exception{

    }
}