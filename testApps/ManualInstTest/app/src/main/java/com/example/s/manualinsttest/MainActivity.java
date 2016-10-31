package com.example.s.manualinsttest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import edu.colorado.plv.tracerunner_runtime_instrumentation.TraceRunnerRuntimeInstrumentation;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Class<? extends MainActivity> aClass = this.getClass();
        Method[] methods = aClass.getDeclaredMethods();
        List<Method> l = new ArrayList<Method>();
        for(Method method: methods){
            if(method.getName().equals("onCreate")){
                l.add(method);
            }
        }


        class Local {};
        Method m = Local.class.getEnclosingMethod();


        TraceRunnerRuntimeInstrumentation.logCallbackEntry("com.example.s.manualinsttest","void onCreate(android.os.Bundle)",new Object[]{this,savedInstanceState});
        setContentView(R.layout.activity_main);

    }
}
